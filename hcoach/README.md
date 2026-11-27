# H Coach Vietnam – Cấu trúc Multi-Page (Mỗi trang 1 file HTML)

Mở `index.html` bằng trình duyệt hoặc chạy qua **Live Server** (cổng `5500`) trong VS Code.
Mỗi màn hình là 1 file `.html` độc lập; khi chuyển trang trình duyệt sẽ **reload sang file HTML mới** và dữ liệu đặt vé được đồng bộ tự động qua `localStorage` + URL query params.

```
hcoach/
├── index.html                    Trang chủ (Home, tìm chuyến, tuyến phổ biến, deals)
├── search.html                   Trang kết quả tìm kiếm chuyến xe (bộ lọc, sắp xếp)
├── seats.html                    Trang chọn ghế (sơ đồ tầng dưới / tầng trên)
├── passenger.html                Trang nhập thông tin hành khách & mã giảm giá
├── payment.html                  Trang thanh toán (Card, MoMo/ZaloPay, VietQR...)
├── confirmation.html             Trang xác nhận đặt vé & mã QR e-ticket
├── trips.html                    Trang quản lý chuyến đi (My Trips)
├── css/
│   ├── 01-base.css               Biến màu (design tokens), reset
│   ├── 02-components.css         Layout chung, nút, badge, ô nhập liệu
│   ├── 03-header.css             Header & menu
│   ├── 04-hero-search.css        Banner trang chủ, thẻ tìm kiếm, dropdown chọn tỉnh
│   ├── 05-home-sections.css      Các khối trang chủ: tuyến phổ biến, deals, đánh giá, footer
│   ├── 06-search-results.css     Màn hình kết quả tìm kiếm
│   ├── 07-seat-map.css           Thanh bước (stepper) & sơ đồ ghế
│   └── 08-payment-modals.css     Thanh toán, vé xác nhận, modal, toast, tiện ích
└── js/
    ├── data/
    │   ├── 01-provinces-stations.js    63 tỉnh, bến xe, hàm định dạng
    │   └── 02-routes-promos-reviews.js Tuyến phổ biến, mã giảm giá, deal, đánh giá
    ├── core/
    │   ├── 03-state.js                 Quản lý state + lưu/đọc localStorage giữa các trang
    │   ├── 04-icons-illustrations.js   Icon SVG, hình xe, ảnh địa danh, mã QR
    │   ├── 05-toast-clipboard.js       Thông báo toast & copy
    │   └── 06-trip-seat-generator.js   Sinh chuyến xe & sơ đồ ghế giả lập
    ├── components/
    │   ├── 07-header-stepper-auth.js   Header (link <a href>), stepper, modal đăng nhập
    │   └── 08-city-picker.js           Dropdown chọn tỉnh/thành (chỉ nạp ở index.html)
    ├── screens/
    │   ├── 09-home.js                  Giao diện index.html
    │   ├── 10-search-results.js        Giao diện search.html
    │   ├── 11-seat-selection.js        Giao diện seats.html
    │   ├── 12-passenger-details.js     Giao diện passenger.html
    │   ├── 13-payment.js               Giao diện payment.html
    │   ├── 14-confirmation.js          Giao diện confirmation.html
    │   └── 15-my-trips.js              Giao diện trips.html
    ├── 16-actions.js                   Xử lý sự kiện & chuyển trang (reload sang file .html mới)
    └── 17-main.js                      Khởi động đúng trang hiện tại & kiểm tra dữ liệu đầu vào
```

## Luồng chuyển trang (Multi-Page Reload)
1. `index.html` → bấm **Search Trips** hoặc chọn tuyến phổ biến → reload sang `search.html?from=...&to=...&date=...&passengers=...`
2. `search.html` → bấm **Select Seats** → reload sang `seats.html`
3. `seats.html` → chọn đủ ghế & bấm **Continue** → reload sang `passenger.html`
4. `passenger.html` → điền thông tin & bấm **Continue to payment** → reload sang `payment.html`
5. `payment.html` → bấm **Pay securely** → reload sang `confirmation.html`
6. `confirmation.html` hoặc menu **My Trips** → reload sang `trips.html`
