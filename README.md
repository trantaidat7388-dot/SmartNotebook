# SmartNotebook

Ứng dụng ghi chú thông minh được xây dựng với JavaFX và SQL Server.

## 📁 Cấu trúc dự án

```
SmartNotebook/
├── .vscode/                      # Cấu hình chạy của VS Code
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/dat/notebook/
│   │   │       ├── App.java          # File chạy chính
│   │   │       ├── controller/       # Xử lý sự kiện giao diện
│   │   │       ├── model/            # Định nghĩa đối tượng Note, Category
│   │   │       └── util/             # Kết nối SQL Server
│   │   └── resources/
│   │       ├── views/                # File giao diện .fxml
│   │       └── css/                  # File làm đẹp .css
├── pom.xml                           # Khai báo thư viện Maven
└── README.md
```

## 🚀 Yêu cầu hệ thống

- Java 17 trở lên
- Maven 3.6+
- SQL Server

## 📦 Cách chạy

```bash
mvn clean javafx:run
```

## 🗄️ Cấu hình Database (SQL Server)

- Chạy script tạo DB: [sql/create_database.sql](sql/create_database.sql)
- Cấu hình kết nối trong: [src/main/resources/db.properties](src/main/resources/db.properties)
- Hoặc set biến môi trường (ưu tiên hơn file):
	- `SMARTNOTEBOOK_DB_SERVER`
	- `SMARTNOTEBOOK_DB_PORT`
	- `SMARTNOTEBOOK_DB_NAME`
	- `SMARTNOTEBOOK_DB_USER`
	- `SMARTNOTEBOOK_DB_PASSWORD`

Nếu chưa cấu hình DB, app vẫn chạy (chế độ offline) và dùng dữ liệu in-memory.

## 📝 Tính năng

- [ ] Tạo, sửa, xóa ghi chú
- [ ] Phân loại ghi chú theo Category
- [ ] Tìm kiếm ghi chú
- [ ] Lưu trữ vào SQL Server
