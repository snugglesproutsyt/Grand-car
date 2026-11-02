# Apex Trigger 3D — Tactical Cyber Ops

An action-packed 3D first-person shooter (FPS) game built natively with **Kotlin** and **Jetpack Compose** for Android.

![Apex Trigger 3D](app/src/main/res/drawable/ic_launcher_foreground.xml)

---

## Features

- **Real-Time 3D Raycasting Engine:**
  - High-performance software raycaster rendering at 60 FPS using DDA wall intersection.
  - Dynamic real-time muzzle flash lighting that dynamically illuminates nearby walls, ceilings, and floors.
  - Z-buffered 3D sprite projection for enemies, projectiles, items, and floating combat text.
  - Atmospheric cyberpunk skybox with distant city silhouettes and perspective floor grid.
  - Full pitch camera adjustment (look up/down), camera head bobbing, and screen recoil.

- **5 Distinct First-Person Weapons:**
  - **Nova-9 Tactical:** Semi-auto sidearm with laser sight and fast reload.
  - **V-7 Sentinel Rifle:** Full-auto assault carbine with holographic red dot sight.
  - **Devastator-12:** Heavy combat shotgun with 8-pellet buckshot spread and pump-action animation.
  - **Reaper X Plasma Sniper:** High-damage rail rifle with a full-screen holographic sniper scope overlay.
  - **Havoc RPG Launcher:** Heavy ordnance with area-of-effect explosive radius.
  - Animated first-person viewmodels with recoil kick, muzzle flash bursts, and ejected brass shell casings.

- **Combat AI & Enemy Bots:**
  - **Scout Drones:** Hovering dual-rotor units firing rapid plasma bursts.
  - **Cyber Enforcers:** Tactical humanoid units with assault rifles that strafe and seek cover.
  - **Titan Boss Mech:** Armored boss with dual shoulder missile pods and rocket barrages.

- **3 Game Modes & 3 Arenas:**
  - **Wave Defense:** Survive 10 escalating waves culminating in Boss Mech battles.
  - **Skirmish 3D:** Fast-paced deathmatch arena (first to 25 eliminations).
  - **Firing Range:** Target practice drills with unlimited ammunition.
  - **Arenas:** Neo-Tokyo Outpost, Bio-Lab Facility, and Titan Colosseum.

- **Cyber Armory & Progression:**
  - Persistent upgrade system for Damage Overcharge, Extended Magazines, and Quick Reloads.
  - Procedural sound synthesizer (AudioTrack) for realistic gunshots, explosions, and reload clicks with zero external audio assets.
  - Haptic feedback pulses for shots, impacts, and damage.

---

## Mobile Controls

- **Left Thumb:** Floating virtual joystick for movement and strafing.
- **Right Thumb:** Swipe across the screen to aim (360° rotation and vertical pitch).
- **FIRE Button:** Hold for automatic fire or tap for single shot.
- **ADS Button:** Toggle Aim Down Sights (zooms FOV and activates sniper scope).
- **SPRINT & CROUCH:** Toggle movement speed and posture.
- **RELOAD Button:** Quick reload with circular progress indicator.
- **Weapon Selector:** Quick-switch between all unlocked weapons.

---

## Building & Running

### Requirements
- Android Studio Ladybug or newer
- JDK 17 / 21
- Android SDK 34+ (minSdk: 24, compileSdk: 36)

### Build Commands
```bash
# Build Debug APK
gradle assembleDebug

# Run Unit & Robolectric Tests
gradle :app:testDebugUnitTest
```

The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`
