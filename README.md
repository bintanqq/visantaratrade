# VisantaraTrade - Advanced Trading Plugin

## 📋 Overview
VisantaraTrade adalah plugin trading system yang canggih dan aman untuk Minecraft 1.21 (Spigot/Paper). Plugin ini menyediakan sistem trading antar pemain dengan GUI yang fleksibel, integrasi ekonomi Vault, dan logging database SQLite.

## ✨ Features

### Core Features
- **Double Chest GUI (54 slots)** dengan layout yang dapat dikustomisasi
- **Economy Integration** dengan Vault API untuk trading uang
- **Distance Check** dengan jarak maksimal yang dapat diatur
- **Cooldown System** untuk mencegah spam trade request
- **Request Expiry** dengan timer yang dapat dikonfigurasi
- **Toggle System** untuk mematikan/menghidupkan permintaan trade
- **Anti-Dupe Protection** yang ketat dengan pengecekan inventory sebelum trade

### Advanced Features
- **Shulker Box Preview** - Hover mouse untuk melihat isi shulker box
- **SQLite Database** untuk logging semua transaksi dengan detail items
- **Sound Effects** untuk setiap aksi trading
- **Blacklist System** untuk world dan item tertentu
- **Admin Commands** untuk melihat riwayat trading lengkap dengan items
- **Multi-Config System** (config.yml, messages.yml, gui.yml)
- **Pay Command** - Transfer uang langsung tanpa GUI
- **Manual Drag & Drop** - Kontrol penuh atas item placement
- **Trade Summary** - Detail lengkap apa yang ditukar setelah trade selesai
- **Customizable Prefix** - Prefix untuk semua messages dapat diubah

## 📦 Installation

### Requirements
- Minecraft Server 1.21 (Paper/Spigot)
- Java 21+
- Vault Plugin
- Economy Plugin (compatible dengan Vault)

### Setup
1. Download file `VisantaraTrade.jar`
2. Letakkan file di folder `plugins/`
3. Pastikan Vault dan economy plugin sudah terinstall
4. Restart server
5. Konfigurasi file di folder `plugins/VisantaraTrade/`

## 🎮 Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/trade <player>` | Send trade request | `visantara.trade.use` |
| `/trade accept <player>` | Accept trade request | `visantara.trade.use` |
| `/trade deny <player>` | Deny trade request | `visantara.trade.use` |
| `/trade toggle` | Toggle trade requests | `visantara.trade.toggle` |
| `/trade logs <player>` | View detailed trade logs (Admin) | `visantara.trade.admin` |
| `/trade reload` | Reload configuration (Admin) | `visantara.trade.admin` |
| `/pay <player> <amount>` | Send money directly | `visantara.pay` |

## 🔐 Permissions

- `visantara.trade.use` - Menggunakan sistem trading (default: true)
- `visantara.trade.toggle` - Toggle trade requests (default: true)
- `visantara.trade.admin` - Admin features + bypass cooldown (default: op)
- `visantara.pay` - Send money to other players (default: true)
- `visantara.trade.*` - Semua permissions

## ⚙️ Configuration

### config.yml
```yaml
settings:
  max-distance: 10.0              # Jarak maksimal untuk trading
  cooldown-seconds: 5             # Cooldown antar request
  request-expiry-seconds: 30      # Durasi request sebelum expired
  money-increment: 100.0          # Increment uang per klik
  drop-items-on-full-inventory: true  # Drop item jika inventory penuh
  
  blacklisted-worlds:             # World yang disabled untuk trading
    - world_nether
    
  blacklisted-items:              # Item yang tidak bisa di-trade
    - BEDROCK
    - BARRIER
```

### messages.yml
Semua message dapat dikustomisasi dengan color codes (`&a`, `&c`, dll).

### gui.yml
Konfigurasi lengkap untuk:
- Slot layout untuk Player 1 dan Player 2
- Filler/separator items
- Button positions (Ready, Money controls)
- Item materials, names, dan lore

## 🗄️ Database Structure

Plugin menggunakan SQLite dengan tabel `trade_logs`:
- `id` - Auto increment primary key
- `player1_uuid` - UUID Player 1
- `player1_name` - Nama Player 1
- `player2_uuid` - UUID Player 2
- `player2_name` - Nama Player 2
- `player1_items` - Serialized items Player 1
- `player2_items` - Serialized items Player 2
- `player1_money` - Jumlah uang Player 1
- `player2_money` - Jumlah uang Player 2
- `timestamp` - Waktu transaksi

## 🏗️ Project Structure

```
me.bintanq.visantaratrade/
├── VisantaraTrade.java          # Main plugin class
├── commands/
│   └── TradeCommand.java        # Command handler
├── listeners/
│   └── TradeListener.java       # Event listeners
├── managers/
│   ├── ConfigManager.java       # Config management
│   ├── MessageManager.java      # Message & sound handling
│   ├── GUIManager.java          # GUI creation & management
│   ├── TradeManager.java        # Trade session management
│   ├── DatabaseManager.java     # SQLite operations
│   └── CooldownManager.java     # Cooldown tracking
└── session/
    └── TradeSession.java        # Trade session logic
```

## 🛡️ Security Features

### Anti-Dupe Protection
1. **Lock System** - Trade di-lock saat kedua player ready
2. **Inventory Verification** - Cek space sebelum complete trade
3. **Money Verification** - Cek balance sebelum transfer
4. **Disconnect Handling** - Auto-cancel jika player disconnect
5. **Server Stop** - Return items saat server shutdown

### Additional Security
- Async database operations untuk performa
- Thread-safe data structures (ConcurrentHashMap)
- Proper inventory transaction handling
- Item serialization untuk logging

## 🎨 Customization Examples

### Custom GUI Layout
Ubah `gui.yml` untuk mengatur slot layout sesuai keinginan:
```yaml
player1-slots: [0, 1, 2, 9, 10, 11]  # Slots untuk Player 1
player2-slots: [6, 7, 8, 15, 16, 17] # Slots untuk Player 2
filler-slots: [3, 4, 5]               # Separator
```

### Custom Sounds
Tambahkan sound effects di `config.yml`:
```yaml
sounds:
  TRADE_SUCCESS:
    enabled: true
    sound: ENTITY_PLAYER_LEVELUP
    volume: 1.0
    pitch: 1.0
```

## 🐛 Troubleshooting

### Trade tidak berfungsi
- Pastikan Vault terinstall dengan benar
- Cek permission player
- Verifikasi tidak ada conflict dengan plugin lain

### Item hilang
- Periksa `drop-items-on-full-inventory` setting
- Cek database logs untuk tracking
- Pastikan tidak ada server crash saat trade

### Database error
- Pastikan folder plugin writable
- Cek file `trades.db` di folder plugin
- Review console error messages

## 📝 Development Notes

### Build Requirements
- Java Development Kit (JDK) 21
- Spigot API 1.21
- Vault API
- Maven/Gradle (build tool)

### Dependencies (pom.xml / build.gradle)
```xml
<dependencies>
    <dependency>
        <groupId>org.spigotmc</groupId>
        <artifactId>spigot-api</artifactId>
        <version>1.21-R0.1-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>com.github.MilkBowl</groupId>
        <artifactId>VaultAPI</artifactId>
        <version>1.7</version>
    </dependency>
    <dependency>
        <groupId>org.xerial</groupId>
        <artifactId>sqlite-jdbc</artifactId>
        <version>3.42.0.0</version>
    </dependency>
</dependencies>
```

## 🤝 Contributing
Feel free to contribute dengan membuat pull request atau melaporkan bug.

## 📄 License
This project is licensed under MIT License.

## 👨‍💻 Author
**Bintanq** - [GitHub Profile](https://github.com/bintanq)

## 🙏 Credits
- Spigot/Paper API
- Vault API
- SQLite Database

---

**Version:** 1.0.0  
**Last Updated:** 2025  
**Support:** Create an issue on GitHub