# UltimatePachinko v1.7.2 Migration Notes

- Base: v1.7.0 advanced feature set (stages/weights/fever/ST/時短, per-machine ball, broadcast, payout cap).
- Added: `/파칭코 구슬지급 [닉] [개수]` command.
- Config: Added `jackpotChance` (fallback) and ensured `centerSlot`, `fever`, `jpachinko.*` keys exist.
- Save format: uses `machines.yml` with per-machine base/gold/diamond/coal and weights (v1.7.0 style).
- If you used v1.7.1's minimal format (`world/x/y/z`), please re-register machines (`/파칭코 생성 <번호>`) or convert manually.
