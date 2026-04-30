# Race Link

Heads-up drag racing for Android. Pairs two phones over Bluetooth and runs a
synchronized Christmas tree to start a 1/8- or 1/4-mile race from a standing
or rolling start. Each phone uses GPS to detect when its car crosses the
finish line.

## How it works

1. **Pair** — both drivers open the app. One taps **Host**, the other taps
   **Scan** and connects. RFCOMM over a fixed UUID, so any Android device
   that has the app can pair; no internet required.
2. **Configure** — the requesting driver picks rolling vs. standing,
   roll-speed (e.g. 40 mph), sync tolerance (±3 mph), and distance. A request
   is sent to the other driver who can **Accept** or **Decline**.
3. **Arm** — both drivers tap "I'm ready". For a rolling start, both speeds
   must be inside the tolerance window (and within tolerance of each other)
   before the host arms the tree.
4. **Tree** — host picks a small randomized delay, then publishes an
   absolute "green at" timestamp. Phones use a tiny NTP-style clock-sync
   layer to fire the green light simultaneously.
5. **Race** — each phone independently integrates GPS distance from green to
   the configured finish line (interpolated across the final segment), then
   broadcasts its own elapsed time. Whoever's lower wins.

## Project layout

```
app/src/main/java/com/racelink/app/
  MainActivity.kt          // entry, screen routing
  AppViewModel.kt          // glues the link, tracker, and engine
  bluetooth/
    BluetoothLink.kt       // discovery + RFCOMM socket, line-delimited JSON
    Messages.kt            // wire protocol
  location/
    SpeedTracker.kt        // FusedLocationProvider wrapper, mph stream
  race/
    RaceConfig.kt          // settings users pick
    ClockSync.kt           // ping/pong offset estimation
    RaceEngine.kt          // state machine: ARMED -> TREE -> RUNNING -> ...
  ui/
    HomeScreen.kt
    PairingScreen.kt
    RaceConfigScreen.kt
    RaceScreen.kt
    InboundRequestDialog.kt
    theme/Theme.kt
```

## Build

Open in Android Studio (Hedgehog or newer). Min SDK 26, target 34. The first
build will download the Compose BoM and Play Services Location.

## Permissions

- `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` (Android 12+)
- `ACCESS_FINE_LOCATION` (always — needed for GPS speed; pre-31 also needed
  for BT discovery)

## Limits / next steps

- No foreground service yet, so a long pre-race wait could be killed if the
  user backgrounds the app for several minutes. The location request is high
  priority but the engine itself runs in the ViewModel scope.
- Finish detection is integrated GPS distance, not a heading-corrected line
  crossing. Within ~5–10 ft on a typical phone, but not NHRA-grade.
- No safety/legal warnings UI. Don't street race.
