# Shield Status — Minecraft 1.16.5 Forge port

Ported from the supplied ShieldStatus 1.19.2 Fabric JAR.

- Minecraft 1.16.5
- Forge 36.2.39
- Java 8 target
- Client-side
- G toggles the indicator
- Green = shield active; red = shield temporarily disabled
- Optional interpolation in the Forge config
- Indicator is intentionally small and only rendered for the local player

Build with the ForgeGradle project using Java 8 and `gradlew build` (wrapper not included in this environment).
