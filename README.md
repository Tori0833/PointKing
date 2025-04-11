# PointKing
## Features

- Track, add, remove, or set points for players
- Automatically handles point transfer on player kills
- View your own or others’ points
- Works with both online and offline players
- Super easy to set up and customize
  
## Commands

| Command | What it does | Who can use it |
|---------|---------------|----------------|
| `/points` | View your own points | Everyone |
| `/points <player>` | View another player’s points | Admin or if allowed in config |
| `/points set <player> <amount>` | Set a player’s points | Admin only |
| `/points add <player> <amount>` | Add points to a player | Admin only |
| `/points remove <player> <amount>` | Remove points from a player | Admin only |
| `/points reload` | Reload config | Admin only |
| `/points top` | Show top players by points | Admin or if allowed in config |

## Permissions

- `pointking.admin`: Allows full access to all commands.

## Config (`config.yml`)

```yaml
starting-points: 100
points-stolen-on-kill: 20
allow-check-others: false
on-zero-points-command: "ban %player% you ran out of point!"
```

- `starting-points`: How many points each new player starts with
- `points-stolen-on-kill`: Does what it does
- `allow-check-others`: Allow non-admins to check others' points
- `on-zero-points-command`: Command to run when a player drops to 0 points (%player% gets replaced as username)

## Installation
1. Drop `.jar` file into your `plugins` folder
2. Start your server
3. Done
