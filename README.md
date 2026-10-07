# NetScope

NetScope is a Kotlin + Jetpack Compose Android network analyzer focused on Wi-Fi, mobile signal, speed testing, history, and AR room signal scanning.

## Implemented
- Live connected Wi-Fi RSSI, SSID and frequency
- 4G/5G signal reading
- Nearby Wi-Fi scan with RSSI/frequency/BSSID
- Download + upload speed test against Cloudflare speed endpoints
- Ping and jitter measurements
- Room database history
- Real ARCore camera session with pose tracking
- Periodic Wi-Fi signal sampling while walking
- World-positioned scan data persisted as x/y/z measurements
- Live AR signal trail with green/yellow/red signal points
- Permission handling for camera, location, phone state and nearby Wi-Fi
- GitHub Actions APK build

## AR room scan
Use a supported ARCore device. Open **AR Scan**, grant camera/network permissions, then walk slowly around the room. NetScope samples the current Wi-Fi RSSI roughly every 700 ms and stores the ARCore camera position with each sample.

AR support is optional at install time, so devices without ARCore can still use the normal network features.

## Limitations
- ARCore must be supported/installed on the phone for room scanning.
- Android Wi-Fi scan results are subject to OS permissions and scan throttling.
- The AR overlay is a lightweight signal trail rather than a full architectural 3D floor-plan reconstruction.
- Speed-test values depend on network conditions and the test endpoint.
