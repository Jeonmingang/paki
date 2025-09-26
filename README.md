
# UltimatePachinko v2.12.3 (Spigot/Paper 1.16.5, Java 8)

**Build fixes (compared to your CI log):**
- Removed use of `Map<?,?>.getOrDefault(...)` that caused `incompatible types: int/double cannot be converted to capture#?` on Java 8.
  - Replaced with safe `Object -> Number` checks everywhere (default stages + saved stages).
- Removed direct reference to `Sound.BLOCK_HOPPER_INSIDE` and replaced with runtime lookup + fallbacks.
  - Works on 1.16.5; compiles against Spigot-API 1.16.5.

All gameplay features from earlier versions are preserved (tokens/coal batch, stage enter/upgrade, personal titles, diamond payout, machine lock, ranking, broadcasts, non-center hopper entry message, etc.).

**Build:**
```
mvn -q -DskipTests clean package
```
