package com.networkar.app.ar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import android.util.AttributeSet
import android.view.Surface
import android.view.View
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.networkar.app.network.NetworkScanner
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

class ArCameraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val onSample: (x: Float, y: Float, z: Float, dbm: Int) -> Unit = { _, _, _, _ -> },
    private val onStatus: (String) -> Unit = {}
) : GLSurfaceView(context, attrs),
    GLSurfaceView.Renderer,
    SurfaceTexture.OnFrameAvailableListener {

    private val appContext =
        context.applicationContext

    private val networkScanner =
        NetworkScanner(appContext)

    private var session: Session? = null

    private var cameraTexture: SurfaceTexture? = null

    private var cameraTextureId = -1

    private var program = 0

    private var positionHandle = -1
    private var texCoordHandle = -1
    private var textureHandle = -1

    private var positionBuffer: FloatBuffer? = null
    private var textureBuffer: FloatBuffer? = null

    private val viewMatrix =
        FloatArray(16)

    private val projectionMatrix =
        FloatArray(16)

    private val textureTransform =
        FloatArray(16)

    private val cameraTexCoords =
        FloatArray(8)

    private val cameraTextureCoords =
        FloatArray(8)

    private val frameAvailable =
        AtomicBoolean(false)

    private var resumed = false

    private var closed = false

    private var lastSampleTime = 0L

    private var lastX = Float.NaN
    private var lastY = Float.NaN
    private var lastZ = Float.NaN

    companion object {

        private const val SAMPLE_INTERVAL_MS =
            500L

        private const val POSITION_CHANGE_THRESHOLD =
            0.05f

        private const val VERTEX_SHADER = """
            attribute vec4 a_Position;
            attribute vec2 a_TexCoord;

            uniform mat4 u_TextureTransform;

            varying vec2 v_TexCoord;

            void main() {
                gl_Position = a_Position;

                vec4 transformed =
                    u_TextureTransform *
                    vec4(a_TexCoord, 0.0, 1.0);

                v_TexCoord =
                    transformed.xy;
            }
        """

        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require

            precision mediump float;

            uniform samplerExternalOES u_Texture;

            varying vec2 v_TexCoord;

            void main() {
                gl_FragColor =
                    texture2D(
                        u_Texture,
                        v_TexCoord
                    );
            }
        """

        private val QUAD_COORDS = floatArrayOf(
            -1f, -1f,
             1f, -1f,
            -1f,  1f,
             1f,  1f
        )

        private val TEX_COORDS = floatArrayOf(
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f
        )
    }

    init {

        setEGLContextClientVersion(2)

        setRenderer(this)

        renderMode =
            GLSurfaceView.RENDERMODE_CONTINUOUSLY

        preserveEGLContextOnPause = true

        positionBuffer =
            createFloatBuffer(QUAD_COORDS)

        textureBuffer =
            createFloatBuffer(TEX_COORDS)

        cameraTexCoords[0] = 0f
        cameraTexCoords[1] = 0f
        cameraTexCoords[2] = 1f
        cameraTexCoords[3] = 0f
        cameraTexCoords[4] = 0f
        cameraTexCoords[5] = 1f
        cameraTexCoords[6] = 1f
        cameraTexCoords[7] = 1f
    }

    // ----------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------

    fun resumeAr() {

        if (closed) {
            return
        }

        if (
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            postStatus(
                "Camera permission required"
            )
            return
        }

        queueEvent {

            try {

                if (session == null) {

                    createSession()

                }

                session?.resume()

                resumed = true

                postStatus(
                    "AR camera started. Move phone slowly to initialize tracking."
                )

            } catch (e: Exception) {

                resumed = false

                postStatus(
                    "AR start failed: ${e.message ?: "Unknown error"}"
                )
            }
        }

        onResume()
    }

    fun pauseAr() {

        if (closed) {
            return
        }

        queueEvent {

            try {

                session?.pause()

                resumed = false

                postStatus(
                    "Scan paused"
                )

            } catch (_: Exception) {
                // Ignore lifecycle race.
            }
        }

        onPause()
    }

    fun closeAr() {

        if (closed) {
            return
        }

        closed = true
        resumed = false

        queueEvent {

            try {

                session?.close()

            } catch (_: Exception) {
                // Ignore close errors.
            }

            session = null

            cameraTexture?.release()
            cameraTexture = null
        }

        try {
            onPause()
        } catch (_: Exception) {
        }
    }

    // ----------------------------------------------------------------
    // ARCore Session
    // ----------------------------------------------------------------

    private fun createSession() {

        if (session != null) {
            return
        }

        val availability =
            ArCoreApk.getInstance()
                .checkAvailability(appContext)

        if (!availability.isSupported) {

            postStatus(
                "ARCore is not supported on this device"
            )

            return
        }

        val newSession =
            try {

                Session(appContext)

            } catch (e: Exception) {

                postStatus(
                    "Unable to create AR session: ${e.message}"
                )

                return
            }

        val config =
            Config(newSession)

        config.focusMode =
            Config.FocusMode.AUTO

        config.planeFindingMode =
            Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL

        config.lightEstimationMode =
            Config.LightEstimationMode.AMBIENT_INTENSITY

        newSession.configure(config)

        session = newSession

        postStatus(
            "ARCore session ready"
        )
    }

    // ----------------------------------------------------------------
    // GLSurfaceView Renderer
    // ----------------------------------------------------------------

    override fun onSurfaceCreated(
        gl: javax.microedition.khronos.opengles.GL10?,
        config: javax.microedition.khronos.egl.EGLConfig?
    ) {

        GLES20.glClearColor(
            0f,
            0f,
            0f,
            1f
        )

        createCameraTexture()

        program =
            createProgram(
                VERTEX_SHADER,
                FRAGMENT_SHADER
            )

        positionHandle =
            GLES20.glGetAttribLocation(
                program,
                "a_Position"
            )

        texCoordHandle =
            GLES20.glGetAttribLocation(
                program,
                "a_TexCoord"
            )

        textureHandle =
            GLES20.glGetUniformLocation(
                program,
                "u_Texture"
            )

        val transformHandle =
            GLES20.glGetUniformLocation(
                program,
                "u_TextureTransform"
            )

        Matrix.setIdentityM(
            textureTransform,
            0
        )

        GLES20.glUseProgram(program)

        GLES20.glUniformMatrix4fv(
            transformHandle,
            1,
            false,
            textureTransform,
            0
        )

        GLES20.glDisable(
            GLES20.GL_DEPTH_TEST
        )

        GLES20.glDisable(
            GLES20.GL_CULL_FACE
        )
    }

    override fun onSurfaceChanged(
        gl: javax.microedition.khronos.opengles.GL10?,
        width: Int,
        height: Int
    ) {

        GLES20.glViewport(
            0,
            0,
            width,
            height
        )

        if (height > 0) {

            val aspect =
                width.toFloat() /
                    height.toFloat()

            Matrix.perspectiveM(
                projectionMatrix,
                0,
                60f,
                aspect,
                0.01f,
                100f
            )
        }

        session?.setDisplayGeometry(
            Surface.ROTATION_0,
            width,
            height
        )
    }

    override fun onDrawFrame(
        gl: javax.microedition.khronos.opengles.GL10?
    ) {

        GLES20.glClear(
            GLES20.GL_COLOR_BUFFER_BIT
        )

        val currentSession =
            session
                ?: return

        if (!resumed) {
            return
        }

        try {

            currentSession.setCameraTextureName(
                cameraTextureId
            )

            val frame =
                currentSession.update()

            frameAvailable.set(false)

            updateTextureTransform(frame)

            renderCameraBackground()

            processFrame(
                frame
            )

        } catch (e: Exception) {

            postStatus(
                "AR frame error: ${e.message ?: "Unknown error"}"
            )
        }
    }

    // ----------------------------------------------------------------
    // Camera texture
    // ----------------------------------------------------------------

    private fun createCameraTexture() {

        val textures =
            IntArray(1)

        GLES20.glGenTextures(
            1,
            textures,
            0
        )

        cameraTextureId =
            textures[0]

        GLES20.glBindTexture(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            cameraTextureId
        )

        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR
        )

        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )

        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )

        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )

        cameraTexture =
            SurfaceTexture(
                cameraTextureId
            )

        cameraTexture?.setOnFrameAvailableListener(
            this
        )
    }

    override fun onFrameAvailable(
        surfaceTexture: SurfaceTexture?
    ) {

        frameAvailable.set(true)
    }

    // ----------------------------------------------------------------
    // Camera background rendering
    // ----------------------------------------------------------------

    private fun renderCameraBackground() {

        if (program == 0) {
            return
        }

        GLES20.glUseProgram(
            program
        )

        GLES20.glDisable(
            GLES20.GL_DEPTH_TEST
        )

        GLES20.glDisable(
            GLES20.GL_CULL_FACE
        )

        positionBuffer?.position(0)

        GLES20.glEnableVertexAttribArray(
            positionHandle
        )

        GLES20.glVertexAttribPointer(
            positionHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            positionBuffer
        )

        textureBuffer?.position(0)

        GLES20.glEnableVertexAttribArray(
            texCoordHandle
        )

        GLES20.glVertexAttribPointer(
            texCoordHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            textureBuffer
        )

        GLES20.glActiveTexture(
            GLES20.GL_TEXTURE0
        )

        GLES20.glBindTexture(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            cameraTextureId
        )

        GLES20.glUniform1i(
            textureHandle,
            0
        )

        val transformHandle =
            GLES20.glGetUniformLocation(
                program,
                "u_TextureTransform"
            )

        GLES20.glUniformMatrix4fv(
            transformHandle,
            1,
            false,
            textureTransform,
            0
        )

        GLES20.glDrawArrays(
            GLES20.GL_TRIANGLE_STRIP,
            0,
            4
        )

        GLES20.glDisableVertexAttribArray(
            positionHandle
        )

        GLES20.glDisableVertexAttribArray(
            texCoordHandle
        )
    }

    private fun updateTextureTransform(
        frame: Frame
    ) {

        try {

            val input =
                floatArrayOf(
                    0f, 0f,
                    1f, 0f,
                    0f, 1f,
                    1f, 1f
                )

            val output =
                FloatArray(8)

            frame.transformCoordinates2d(
                com.google.ar.core.Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
                input,
                com.google.ar.core.Coordinates2d.TEXTURE_NORMALIZED,
                output
            )

            for (i in 0 until 8) {
                cameraTexCoords[i] =
                    output[i]
            }

            textureBuffer =
                createFloatBuffer(
                    cameraTexCoords
                )

        } catch (_: Exception) {

            // Keep previous texture coordinates.
        }
    }

    // ----------------------------------------------------------------
    // AR frame processing
    // ----------------------------------------------------------------

    private fun processFrame(
        frame: Frame
    ) {

        val camera =
            frame.camera

        when (camera.trackingState) {

            TrackingState.TRACKING -> {

                postStatus(
                    "Tracking OK • Move slowly around the room"
                )

                collectSignalSample(
                    frame
                )
            }

            TrackingState.PAUSED -> {

                postStatus(
                    "Tracking paused • Move phone to a textured area"
                )
            }

            TrackingState.STOPPED -> {

                postStatus(
                    "Tracking stopped"
                )
            }
        }
    }

    private fun collectSignalSample(
        frame: Frame
    ) {

        val now =
            SystemClock.elapsedRealtime()

        if (
            now - lastSampleTime <
            SAMPLE_INTERVAL_MS
        ) {
            return
        }

        val camera =
            frame.camera

        val pose =
            camera.pose

        val x =
            pose.tx()

        val y =
            pose.ty()

        val z =
            pose.tz()

        val positionChanged =
            if (
                lastX.isNaN() ||
                lastY.isNaN() ||
                lastZ.isNaN()
            ) {
                true
            } else {

                abs(x - lastX) >=
                    POSITION_CHANGE_THRESHOLD ||

                abs(y - lastY) >=
                    POSITION_CHANGE_THRESHOLD ||

                abs(z - lastZ) >=
                    POSITION_CHANGE_THRESHOLD
            }

        if (!positionChanged) {
            return
        }

        val wifi =
            networkScanner.currentWifi()

        val dbm =
            wifi?.rssi
                ?.takeIf {
                    com.networkar.app.network.SignalUtils.isValid(it)
                }
                ?: return

        lastSampleTime =
            now

        lastX = x
        lastY = y
        lastZ = z

        postSample(
            x,
            y,
            z,
            dbm
        )
    }

    // ----------------------------------------------------------------
    // Callbacks
    // ----------------------------------------------------------------

    private fun postSample(
        x: Float,
        y: Float,
        z: Float,
        dbm: Int
    ) {

        post {

            try {

                onSample(
                    x,
                    y,
                    z,
                    dbm
                )

            } catch (_: Exception) {
            }
        }
    }

    private fun postStatus(
        message: String
    ) {

        post {

            try {

                onStatus(
                    message
                )

            } catch (_: Exception) {
            }
        }
    }

    // ----------------------------------------------------------------
    // OpenGL helpers
    // ----------------------------------------------------------------

    private fun createFloatBuffer(
        values: FloatArray
    ): FloatBuffer {

        return ByteBuffer
            .allocateDirect(
                values.size * 4
            )
            .order(
                ByteOrder.nativeOrder()
            )
            .asFloatBuffer()
            .apply {
                put(values)
                position(0)
            }
    }

    private fun loadShader(
        type: Int,
        source: String
    ): Int {

        val shader =
            GLES20.glCreateShader(type)

        if (shader == 0) {
            throw IllegalStateException(
                "Unable to create OpenGL shader"
            )
        }

        GLES20.glShaderSource(
            shader,
            source
        )

        GLES20.glCompileShader(
            shader
        )

        val status =
            IntArray(1)

        GLES20.glGetShaderiv(
            shader,
            GLES20.GL_COMPILE_STATUS,
            status,
            0
        )

        if (status[0] == 0) {

            val log =
                GLES20.glGetShaderInfoLog(
                    shader
                )

            GLES20.glDeleteShader(
                shader
            )

            throw IllegalStateException(
                "Shader compilation failed: $log"
            )
        }

        return shader
    }

    private fun createProgram(
        vertexSource: String,
        fragmentSource: String
    ): Int {

        val vertexShader =
            loadShader(
                GLES20.GL_VERTEX_SHADER,
                vertexSource
            )

        val fragmentShader =
            loadShader(
                GLES20.GL_FRAGMENT_SHADER,
                fragmentSource
            )

        val programId =
            GLES20.glCreateProgram()

        if (programId == 0) {

            GLES20.glDeleteShader(
                vertexShader
            )

            GLES20.glDeleteShader(
                fragmentShader
            )

            throw IllegalStateException(
                "Unable to create OpenGL program"
            )
        }

        GLES20.glAttachShader(
            programId,
            vertexShader
        )

        GLES20.glAttachShader(
            programId,
            fragmentShader
        )

        GLES20.glLinkProgram(
            programId
        )

        val status =
            IntArray(1)

        GLES20.glGetProgramiv(
            programId,
            GLES20.GL_LINK_STATUS,
            status,
            0
        )

        if (status[0] == 0) {

            val log =
                GLES20.glGetProgramInfoLog(
                    programId
                )

            GLES20.glDeleteProgram(
                programId
            )

            GLES20.glDeleteShader(
                vertexShader
            )

            GLES20.glDeleteShader(
                fragmentShader
            )

            throw IllegalStateException(
                "OpenGL program link failed: $log"
            )
        }

        GLES20.glDeleteShader(
            vertexShader
        )

        GLES20.glDeleteShader(
            fragmentShader
        )

        return programId
    }

    // ----------------------------------------------------------------
    // View lifecycle
    // ----------------------------------------------------------------

    override fun onDetachedFromWindow() {

        closeAr()

        super.onDetachedFromWindow()
    }
}
