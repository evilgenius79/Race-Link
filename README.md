# Race Link

**Heads-up drag racing for two Android phones.** Pair over Bluetooth, pick
a start type and distance, and run a synchronized Christmas tree to time a
1/8- or 1/4-mile race. Each phone uses GPS to measure its own car, then the
two phones compare times and crown a winner.

> ⚠️ **For closed-course / track use only.** Don't street race. The author
> assumes no liability for what you do with this app.

---

## What it does

- 🔗 **Bluetooth pairing** between two phones running the app
- 🎛️ **Race setup** chosen by the requester, confirmed by the other driver
  - Standing or rolling start
  - Roll speed: 20–80 mph (rolling only)
  - Sync tolerance: ±1–10 mph
  - Distance: 1/8 mile (660 ft), 1000 ft, or 1/4 mile (1320 ft)
- 🚦 **Synchronized tree** — both phones fire the green light at the *same
  wall-clock instant*, even though they're separate devices. A tiny NTP-style
  clock-sync layer keeps the two clocks aligned.
- 🛣️ **Rolling-start logic** — the tree won't fire until both cars are
  within tolerance of the target speed *and* within tolerance of each
  other.
- ⏱️ **GPS finish detection** — each phone integrates distance traveled
  from the moment the green fires and reports its own time + trap speed.
- 🏁 **Heads-up result** — your time, opponent's time, and the margin.

---

## Screens (text mockups)

> *Real screenshots go here once the app is built. The mockups below show
> the layout; the actual UI is dark-themed Material 3 with a red accent.*

### Home

```
┌──────────────────────────────┐
│                              │
│         RACE LINK            │
│   Heads-up drag racing       │
│                              │
│  ┌────────────────────────┐  │
│  │ Find a car to race  ▶ │  │
│  └────────────────────────┘  │
│  ┌────────────────────────┐  │
│  │ Wait for incoming      │  │
│  └────────────────────────┘  │
│                              │
└──────────────────────────────┘
```
*(If permissions aren't granted, the buttons are replaced with a
"Grant permissions" prompt.)*

### Pairing

```
┌──────────────────────────────┐
│ Pair with another driver     │
│ Scanning for nearby cars...  │
│ ┌──────────┐ ┌──────────┐    │
│ │  Stop    │ │  Host    │    │
│ └──────────┘ └──────────┘    │
│        ◯ scanning...         │
│                              │
│ Paired devices               │
│ ┌──────────────────────────┐ │
│ │ BT  Pixel 7      Connect │ │
│ │     7C:DD:.. .          │ │
│ └──────────────────────────┘ │
│                              │
│ Nearby                       │
│ • Galaxy S24    Connect      │
│ • OnePlus 11    Connect      │
└──────────────────────────────┘
```

### Race setup

```
┌──────────────────────────────┐
│ Set up the race              │
│                              │
│ Start type                   │
│ [ Standing ] [ ✓ Rolling ]   │
│                              │
│ Roll speed: 40 mph           │
│ ───────────●──────           │
│                              │
│ Sync tolerance: ±3 mph       │
│ ──●─────────────             │
│                              │
│ Distance                     │
│ [ 1/8 ] [ ✓ 1/4 ] [ 1000ft ] │
│                              │
│ ┌────────────────────────┐   │
│ │  Send race request     │   │
│ └────────────────────────┘   │
└──────────────────────────────┘
```

### Inbound request (the other phone)

```
        ┌──────────────────┐
        │  Race request    │
        │                  │
        │  Rolling start   │
        │  at 40 mph (±3)  │
        │  1320 ft         │
        │                  │
        │ Decline   Accept │
        └──────────────────┘
```

### Live race

```
┌──────────────────────────────┐
│ Cruise to 40 mph (±3)        │
│                  1320 ft  Host│
│                              │
│  ┌────────────────────────┐  │
│  │          ●             │  │   amber 1
│  │          ●             │  │   amber 2
│  │          ●             │  │   amber 3
│  │          ●             │  │   green
│  │          ○             │  │   red
│  └────────────────────────┘  │
│                              │
│  ┌────────┐  ┌────────┐      │
│  │  YOU   │  │  OPP   │      │
│  │   42   │  │   39   │      │
│  │  mph   │  │  mph   │      │
│  └────────┘  └────────┘      │
│                              │
│ [ I'm ready ]   [ Abort ]    │
│ You: READY      Opp: —       │
└──────────────────────────────┘
```

After the green fires, the speed cards stay live and a distance bar +
elapsed time appear. When both phones cross the finish:

```
┌──────────────────────────────┐
│           WIN                │
│ You: 11.842 s @ 119 mph      │
│ Opp: 12.107 s @ 117 mph      │
│ Margin: +0.265 s             │
│                              │
│ [        Done        ]       │
└──────────────────────────────┘
```

---

## Installing

You need to build it yourself for now (no Play Store release).

### Requirements

- Android Studio Hedgehog (2023.1) or newer
- JDK 17
- Two Android phones, **API 26+** (Android 8.0 Oreo or newer)
- Both phones must have Bluetooth and GPS

### Build & install

```bash
git clone <this repo>
cd Race-Link
# Open in Android Studio, let Gradle sync, then either:
./gradlew installDebug    # with a phone on USB + USB debugging on
# or use Run ▶ in Android Studio
```

Repeat on the second phone (or `adb -s <serial> install app/build/outputs/apk/debug/app-debug.apk`).

### Permissions

On first launch the app asks for:

- **Location (precise)** — required for GPS speed and, on Android ≤ 11,
  for Bluetooth discovery.
- **Nearby devices** (Android 12+) — Bluetooth scan + connect.

Grant both. If you deny, the home screen will keep prompting.

You may also need to **enable Bluetooth and GPS** in system settings.

---

## How to pair two phones

You only need to do the OS-level pairing **once** per pair of phones; after
that the app finds the other side immediately.

1. On Phone A, open Race Link and tap **Find a car to race** → **Host**.
   The phone is now waiting for an inbound connection.
2. On Phone B, open Race Link → **Find a car to race** → **Scan**.
3. Phone A appears in the **Nearby** list (or under **Paired devices** if
   you've connected before). Tap it.
4. Android may ask both users to confirm a pairing PIN the first time —
   accept on both phones.
5. Phone B's status switches to **Connected**. Phone A's does too.
6. Tap **Continue to race setup** on whichever side wants to set the rules.

Tip: either phone can be the host, doesn't matter. Once connected, both
sides can send messages, and either driver can configure and request the
race.

---

## How to run a race

### Rolling start (the typical one)

1. **Driver A** (anyone) taps **Continue to race setup** and chooses:
   - Rolling start
   - Roll speed (e.g. 40 mph)
   - Sync tolerance (e.g. ±3 mph)
   - Distance (e.g. 1/4 mile)
   Then taps **Send race request**.
2. **Driver B** sees a popup. Taps **Accept**.
3. Both phones jump to the live race screen.
4. Both drivers tap **I'm ready** when seated and rolling toward the
   start. The status reads `You: READY  Opp: READY`.
5. Both drivers cruise side-by-side at the target speed. The speed cards
   turn green when each of you is inside the window.
6. Once **both** are inside the window *and* within tolerance of each
   other, the host phone arms the tree and broadcasts the green-light
   time. After a short randomized delay (1.5–3 s) the ambers begin.
7. **Amber, amber, amber, GREEN** — go. Both phones fire green at the
   same wall-clock instant.
8. Each phone times your run and broadcasts its result. The winner sees
   "WIN" in green on their screen. Margin is shown in seconds.

### Standing start

Same flow, but pick **Standing**. The tree won't arm until both speeds
read under 2 mph. Stage the cars, both tap **I'm ready**, sit still, and
the tree fires after a short random delay.

### Aborting

Tap **Abort** on either phone at any time. The other phone sees an
"Aborted" message and you both go back to setup.

---

## Tips & gotchas

- **Phone position matters for GPS.** Lay it on the dash or in a vent
  mount where the antenna can see the sky. A phone face-down on the
  passenger seat will lose accuracy. Allow ~30 s after launching the app
  for the GPS lock to settle before racing.
- **Distance is integrated GPS, not a fixed line.** It measures *your
  car's* distance from green. So your start technique matters: if you
  brake-stand on a rolling start the engine will count distance from the
  moment the green fired, which is the right behavior.
- **Bluetooth Classic range** is ~10 m/30 ft typically. The phones don't
  need to stay connected during the actual run — the tree-fire time and
  the distance target are both already known to each phone. They reconnect
  for the result swap. (In practice they usually stay connected the
  whole time anyway if you're racing side-by-side.)
- **Don't background the app** during a race. Android may aggressively
  throttle GPS updates or kill the process on long pre-race waits.
- **First connection is the fiddly one.** If Scan doesn't see the other
  phone: confirm Bluetooth is on, confirm Location is on (Android needs
  it for BT discovery on older versions), and try toggling **Host** on
  the other side again.

---

## Troubleshooting

| Symptom | Likely cause / fix |
| --- | --- |
| "Need permissions: Bluetooth + Location" on home | Tap **Grant permissions**. If it doesn't re-prompt, open system Settings → Apps → Race Link → Permissions, allow all. |
| Scan finds nothing | Both phones must have Race Link installed and one must be in **Host** mode. Toggle Bluetooth off/on. On Android ≤ 11, Location must be enabled (not just permission granted). |
| "Connect failed: io error" | The other side wasn't actually hosting, or the link was stale. Tap **Host** again, then **Scan / Connect** from the other phone. |
| Tree never fires | (Rolling) one or both speeds aren't getting inside the window. Check the YOU/OPP cards — they should turn green at the target speed. (Standing) one phone is reporting drift speed. Wait for the cards to read 0–1 mph. |
| Both phones disagree on time by >0.1 s | Clock sync needs a second or two of pings to converge. The host now waits up to 2 s for a sync sample before scheduling the tree. If it's still off, restart the race. |
| GPS distance looks short or long | Look at the distance bar during the run. If it under-reads, the GPS likely didn't have a clear sky view at launch. |

---

## Project layout

```
app/src/main/java/com/racelink/app/
  MainActivity.kt          // entry, screen routing, permissions
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

### How the synchronized tree works

Each phone has its own clock — they will drift by tens to hundreds of
milliseconds. The app does a tiny NTP-style exchange continuously:

```
A → B: ping(t1=A_send)
B → A: pong(t1, t2=B_recv, t3=B_send)
A:     t4 = A_recv
       offset = ((t2 - t1) + (t3 - t4)) / 2
       rtt    =  (t4 - t1) - (t3 - t2)
```

A keeps the lowest-RTT sample of the last few rounds as its "best
offset". When the host decides to fire the green at local wallclock T0,
it sends `T0 + offset` to the peer; the peer subtracts its own offset
estimate to get *its* local equivalent of the same instant. Both phones
then run a `delay(T0 - now)` and fire green together.

In practice this gets the two greens within ~10–30 ms of each other on
healthy Bluetooth connections, which is well below human reaction time.

### What's not in v1

- No foreground service — long pre-race waits while the screen is off
  could be killed by the OS.
- No history / saved runs.
- No reaction-time measurement (would need a "stage beam" simulation).
- No false-start / red-light enforcement.
- No internet matchmaking — Bluetooth-only, two phones at a time.

PRs welcome.

---

## License

TBD — pick one (MIT / Apache 2.0 are both fine for this).
