# Anti Lava Logout
This plugin prevents players abusing spawn protection in lava, by logging out every few seconds
## Config
```yaml
base-damage: 15 #damage dealt on login if the player is in lava
  #(take hearts and multiply by 2, 1 half heart = 1 hp)
damage-second: 5 # damage dealt every second the player is logged out
message: '&cYou logged out while in lava for %seconds% and took %damage% damage!'
```
