# Local MapGUI Fork and Delta Rendering Design

## Goal
Use `/home/jlo/dev/MapGUI` for the server integration and ensure MapGUI sends changed map rectangles instead of full frames after initial synchronization.

## Scope
The change is global to MapGUI surfaces: hand-held screens, walls, videos, cameras, and any transport using `MapSurface`. Wizardry's `GlyphScreen` remains an API consumer and does not implement a second diff layer.

## Architecture
The Wizardry Gradle build will include the MapGUI checkout as an included build. `mapgui-integration` will depend on the included build's `mapgui-api` project, and tests will use that same project dependency. The deployed server will use the locally built `MapGUI` shadow jar, not the Maven Central plugin artifact.

`MapSurface` remains the single owner of pixel state and dirty state. A pixel write first compares the existing byte; equal writes do nothing. Changed writes update the pixel and exact tile/row span metadata. `Patches.plan` converts each dirty tile's spans to one or more rectangles using payload area plus packet split cost. Full redraws remain one rectangle, while separated small regions remain separate when cheaper.

`WallDisplay` and the hand-screen transport keep their existing lifecycle: new viewers receive full state, established viewers receive only dirty regions, and dirty state is cleared after the send pass so all viewers see the same committed frame.

## Data Flow
1. A screen, camera, or wall paints into a `MapSurface`.
2. `MapSurface.set` records only actual pixel changes.
3. The transport asks for dirty regions per map tile.
4. The transport copies each region into a map update packet inside the existing bundle.
5. The surface clears dirty metadata after the viewer pass.
6. A new viewer is identified separately and receives a full map synchronization.

## Correctness and Failure Handling
- Out-of-bounds writes remain ignored by the existing `Surface` contract.
- Equal-color writes must not create dirty state.
- Dirty metadata is not cleared before packet creation.
- A full redraw is used for initial viewer synchronization and explicit `markAllDirty` calls.
- Rectangle planning must never return a rectangle outside the surface or a split plan more expensive than its bounding box.

## Verification
Focused MapGUI tests cover equal writes, disjoint tiles, exact row spans, full redraw behavior, rectangle cost bounds, and transport initial/subsequent behavior. Wizardry verification must prove the integration resolves the local included-build API and that the integration test suite and server smoke path build against it.
