package com.networkar.app.ar

import android.app.Activity
import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.view.Surface
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.networkar.app.network.WifiScanner
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicBoolean
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max
import kotlin.math.min

class ArCameraView(
    context: Context,
    private val onSample: (x: Float, y: Float, z: Float, dbm: Int) -> Unit,
    private val onStatus: (String) -> Unit
) : GLSurfaceView(context), GLSurfaceView.Renderer {

    private val activity = context as Activity
    private val wifi = WifiScanner(context)
    private var session: Session? = null
    private var frame: Frame? = null
    private var viewportWidth = 1
    private var viewportHeight = 1
    private var lastRotation = -1
    private var lastSampleMs = 0L
    private val running = AtomicBoolean(false)
    private val renderer = ArPointRenderer()

    init {
        setEGLContextClientVersion(2)
        setRenderer(this)
        renderMode = RENDERMODE_CONTINUOUSLY
        preserveEGLContextOnPause = true
    }

    fun resumeAr() {
        if (!hasCameraPermission()) {
            onStatus("Camera permission is required")
            return
        }
        try {
            val install = ArCoreApk.getInstance().requestInstall(activity, true)
            if (install != ArCoreApk.InstallStatus.INSTALLED) {
                onStatus("Installing ARCore…")
                return
            }
            if (session == null) {
                session = Session(context)
                session?.configure(Config(session).apply {
                    updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                    focusMode = Config.FocusMode.AUTO
                })
            }
            session?.resume()
            if (renderer.textureId >= 0) session?.setCameraTextureName(renderer.textureId)
            super.onResume()
            running.set(true)
            onStatus("AR tracking active — move slowly around the room")
        } catch (e: Exception) {
            onStatus("AR unavailable: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    fun pauseAr() {
        running.set(false)
        try { session?.pause() } catch (_: Exception) { }
        super.onPause()
    }

    fun closeAr() {
        pauseAr()
        session?.close()
        session = null
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        renderer.create()
        session?.setCameraTextureName(renderer.textureId)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        val s = session ?: return
        try {
            val rotation = activity.windowManager.defaultDisplay.rotation
            if (rotation != lastRotation) {
                s.setDisplayGeometry(rotation, viewportWidth, viewportHeight)
                lastRotation = rotation
            }
            val f = s.update()
            frame = f
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            renderer.drawCamera(f)

            val pose = f.camera.pose
            renderer.drawPoints(pose.translation)

            val now = System.currentTimeMillis()
            if (running.get() && now - lastSampleMs >= 700L) {
                lastSampleMs = now
                val info = wifi.current()
                val dbm = info?.rssi ?: -100
                val p = pose.translation
                val x = p[0]
                val y = p[1]
                val z = p[2]
                onSample(x, y, z, dbm)
                renderer.addPoint(x, y, z, dbm)
            }
        } catch (_: CameraNotAvailableException) {
            onStatus("Camera became unavailable — reopen AR Scan")
        } catch (e: Exception) {
            onStatus("AR frame error: ${e.message ?: "unknown"}")
        }
    }

    private fun hasCameraPermission() =
        androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    private class ArPointRenderer {
        var textureId: Int = -1
            private set
        private var cameraProgram = 0
        private var pointProgram = 0
        private var cameraPos = -1
        private var cameraTex = -1
        private var pointMvp = -1
        private var pointPos = -1
        private var pointColor = -1
        private val points = mutableListOf<Point>()
        private val quad: FloatBuffer = floatBuffer(floatArrayOf(
            -1f, -1f, 0f, 1f,
             1f, -1f, 1f, 1f,
            -1f,  1f, 0f, 0f,
             1f,  1f, 1f, 0f
        ))

        fun create() {
            textureId = createExternalTexture()
            cameraProgram = program(CAMERA_VS, CAMERA_FS)
            cameraPos = GLES20.glGetAttribLocation(cameraProgram, "aPosition")
            cameraTex = GLES20.glGetUniformLocation(cameraProgram, "uTexture")
            pointProgram = program(POINT_VS, POINT_FS)
            pointMvp = GLES20.glGetUniformLocation(pointProgram, "uMvp")
            pointPos = GLES20.glGetAttribLocation(pointProgram, "aPosition")
            pointColor = GLES20.glGetUniformLocation(pointProgram, "uColor")
        }

        fun drawCamera(frame: Frame) {
            if (cameraProgram == 0) return
            GLES20.glDisable(GLES20.GL_DEPTH_TEST)
            GLES20.glUseProgram(cameraProgram)
            quad.position(0)
            GLES20.glEnableVertexAttribArray(cameraPos)
            GLES20.glVertexAttribPointer(cameraPos, 2, GLES20.GL_FLOAT, false, 16, quad)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
            GLES20.glUniform1i(cameraTex, 0)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GLES20.glDisableVertexAttribArray(cameraPos)
        }

        fun addPoint(x: Float, y: Float, z: Float, dbm: Int) {
            synchronized(points) {
                points += Point(x, y, z, dbm)
                if (points.size > 250) points.removeAt(0)
            }
        }

        fun drawPoints(cameraPosition: FloatArray) {
            if (pointProgram == 0) return
            // Points are drawn as a simple world-space trail around the tracked camera.
            // The renderer keeps them visible as a lightweight AR heatmap trail.
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            GLES20.glUseProgram(pointProgram)
            synchronized(points) {
                for (p in points) {
                    val dx = p.x - cameraPosition[0]
                    val dy = p.y - cameraPosition[1]
                    val dz = p.z - cameraPosition[2]
                    if (kotlin.math.abs(dz) > 0.05f) {
                        val sx = (dx / max(0.5f, kotlin.math.abs(dz))).coerceIn(-1.2f, 1.2f)
                        val sy = (dy / max(0.5f, kotlin.math.abs(dz))).coerceIn(-1.2f, 1.2f)
                        val mvp = floatArrayOf(
                            1f,0f,0f,0f, 0f,1f,0f,0f, 0f,0f,1f,0f, 0f,0f,0f,1f
                        )
                        mvp[12] = sx * 0.35f
                        mvp[13] = sy * 0.35f
                        GLES20.glUniformMatrix4fv(pointMvp, 1, false, mvp, 0)
                        GLES20.glVertexAttrib4f(pointPos, 0f, 0f, 0f, 1f)
                        val q = qualityColor(p.dbm)
                        GLES20.glUniform4f(pointColor, q[0], q[1], q[2], 0.85f)
                        GLES20.glPointSize(28f)
                        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1)
                    }
                }
            }
            GLES20.glDisable(GLES20.GL_BLEND)
        }

        private fun qualityColor(dbm: Int): FloatArray = when {
            dbm >= -55 -> floatArrayOf(0.1f, 0.95f, 0.45f)
            dbm >= -70 -> floatArrayOf(1f, 0.8f, 0.1f)
            else -> floatArrayOf(1f, 0.18f, 0.18f)
        }

        private fun createExternalTexture(): Int {
            val ids = IntArray(1)
            GLES20.glGenTextures(1, ids, 0)
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, ids[0])
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            return ids[0]
        }

        private fun program(vs: String, fs: String): Int {
            val v = compile(GLES20.GL_VERTEX_SHADER, vs)
            val f = compile(GLES20.GL_FRAGMENT_SHADER, fs)
            val p = GLES20.glCreateProgram()
            GLES20.glAttachShader(p, v); GLES20.glAttachShader(p, f); GLES20.glLinkProgram(p)
            return p
        }

        private fun compile(type: Int, source: String): Int {
            val s = GLES20.glCreateShader(type)
            GLES20.glShaderSource(s, source); GLES20.glCompileShader(s)
            return s
        }

        private data class Point(val x: Float, val y: Float, val z: Float, val dbm: Int)

        companion object {
            private const val CAMERA_VS = """
                attribute vec4 aPosition;
                varying vec2 vTex;
                void main(){ gl_Position=aPosition; vTex=aPosition.xy*0.5+0.5; }
            """
            private const val CAMERA_FS = """
                #extension GL_OES_EGL_image_external : require
                precision mediump float;
                uniform samplerExternalOES uTexture;
                varying vec2 vTex;
                void main(){ gl_FragColor=texture2D(uTexture, vec2(vTex.x, 1.0-vTex.y)); }
            """
            private const val POINT_VS = """
                attribute vec4 aPosition;
                uniform mat4 uMvp;
                void main(){ gl_Position=uMvp*aPosition; }
            """
            private const val POINT_FS = """
                precision mediump float;
                uniform vec4 uColor;
                void main(){ gl_FragColor=uColor; }
            """
        }
    }

    companion object {
        private fun floatBuffer(data: FloatArray): FloatBuffer =
            ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(data); position(0) }
    }
}
