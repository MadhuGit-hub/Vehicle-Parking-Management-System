# 🚗 Vehicle Parking Management System

## 📌 Project Overview

The Vehicle Parking Management System is a software application designed to automate and efficiently manage parking operations. The system helps track vehicle entry and exit, allocate parking slots, calculate parking fees, and maintain records in a structured manner. This reduces manual work and improves parking management.

---

## ❗ Problem Statement

In many places such as malls, offices, hospitals, and educational institutions, parking management is handled manually. This creates several issues:

* Difficulty in finding available parking slots
* Traffic congestion inside parking areas
* Time and fuel wastage
* Manual record-keeping errors
* No proper vehicle tracking
* Inaccurate billing
* Poor security

Therefore, an automated system is required to manage parking efficiently.

---

## 💡 Proposed Solution

The Vehicle Parking Management System automates the entire parking process:

* Automatically records vehicle entry and exit
* Assigns available parking slots
* Tracks slot availability in real-time
* Calculates parking fees automatically
* Maintains vehicle and payment records
* Provides admin control for monitoring

This improves efficiency, reduces errors, and saves time.

---

## 🎯 Objectives

* Automate parking management
* Reduce manual work
* Provide real-time slot availability
* Improve parking space utilization
* Maintain vehicle records
* Generate automatic billing
* Enhance security

---

## 🧩 Modules

1. Admin Module
2. Vehicle Entry Module
3. Slot Management Module
4. Vehicle Exit Module
5. Billing Module
6. Database Module

---

## ⚙️ How It Works

1. Vehicle enters parking area
2. Operator enters vehicle number
3. System checks available slots
4. Slot is assigned automatically
5. Entry time is stored in database
6. Vehicle exits parking area
7. System calculates parking fee
8. Slot becomes available again

---

## 🏗️ System Architecture

* Frontend (User Interface)
* Backend Server (Business Logic)
* Database (Data Storage)
* Slot Management Module

Flow:
Vehicle Entry → Slot Allocation → Database → Vehicle Exit → Fee Calculation → Exit

---

## 🛠️ Technologies Used

* Frontend: (React / HTML / CSS / JS)
* Backend: (Spring Boot / Node.js / Python)
* Database: (MySQL / PostgreSQL)

---

## 🚀 How to Setup Project

### 1. Clone Repository

```bash
git clone https://github.com/your-username/vehicle-parking-management-system.git
cd vehicle-parking-management-system
```

### 2. Setup Database

* Create database in MySQL
* Update database credentials in configuration file

Example:

```
spring.datasource.url=jdbc:mysql://localhost:3306/parking
spring.datasource.username=root
spring.datasource.password=yourpassword
```

### 3. Run Backend

```bash
mvn spring-boot:run
```

### 4. Run Frontend

```bash
npm install
npm start
```

### 5. Open in Browser

```
http://localhost:3000
```

---

## ✅ Features

* Automatic slot allocation
* Entry & exit tracking
* Parking fee calculation
* Admin dashboard
* Real-time availability
* Secure data storage

---

## 📈 Future Enhancements

* QR code entry/exit
* Online slot booking
* Payment gateway integration
* Mobile app support
* IoT-based smart parking sensors

---

## 👨💻 Author

CHERUKURI MADHU

---

## 📄 License

This project is for educational purposes.
