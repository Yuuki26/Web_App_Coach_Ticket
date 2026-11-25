# H Coach Vietnam – cấu trúc thư mục

Mở `index.html` bằng trình duyệt là chạy (không cần build).

```
index.html                  Khung trang: nạp CSS + JS theo thứ tự
css/
  01-base.css               Biến màu (design tokens), reset
  02-components.css         Layout chung, nút, badge, ô nhập liệu
  03-header.css             Header & menu
  04-hero-search.css        Banner trang chủ, thẻ tìm kiếm, dropdown chọn tỉnh
  05-home-sections.css      Các khối trang chủ: tuyến phổ biến, khuyến mãi, đánh giá, footer...
  06-search-results.css     Màn hình kết quả tìm kiếm
  07-seat-map.css           Thanh bước (stepper) & sơ đồ ghế
  08-payment-modals.css     Thanh toán, vé xác nhận, modal, toast, tiện ích
  09-routes.css             Màn hình Tuyến xe (danh sách, bản đồ, giao thông)
js/
  data/
    01-provinces-stations.js    63 tỉnh, bến xe, hàm định dạng (formatVND, ngày...)
    02-routes-promos-reviews.js Tuyến phổ biến, mã giảm giá, deal đếm ngược, đánh giá, nhà xe
  core/
    03-state.js                 Biến `state` – toàn bộ trạng thái ứng dụng
    03b-api.js                  Gọi backend Spring Boot (API_BASE, apiGet)
    04-icons-illustrations.js   Icon SVG, hình xe, ảnh địa danh, mã QR
    05-toast-clipboard.js       Thông báo toast & copy
    06-trip-seat-generator.js   Sinh chuyến xe & sơ đồ ghế giả lập
  components/
    07-header-stepper-auth.js   Header, stepper, modal đăng nhập
    08-city-picker.js           Dropdown chọn tỉnh/thành
  screens/
    09-home.js … 15-my-trips.js Mỗi file = 1 màn hình (render HTML)
    15b-routes.js               Màn hình Tuyến xe: dữ liệu thật từ backend + giao thông trực tiếp
  16-actions.js                 Xử lý sự kiện (onclick: chọn ghế, thanh toán...)
  17-main.js                    Hàm render() chính & khởi động app
```

## Lưu ý quan trọng
- Các file JS là script thường, dùng chung biến toàn cục (`state`, các hàm render...).
  **Không đổi thứ tự** thẻ `<script>` trong `index.html` và **không** thêm `type="module"`,
  nếu không các nút `onclick="..."` sẽ không tìm thấy hàm.
- Luồng chạy: người dùng bấm → hàm trong `16-actions.js` sửa `state` → gọi `render()`
  (`17-main.js`) → vẽ lại màn hình tương ứng trong `screens/`.

## Màn hình Tuyến xe (Routes) – dùng dữ liệu thật từ backend
- Cần chạy backend (`bus-ticket-booking`, cổng 8081) và mở trang qua web server, ví dụ
  VS Code **Live Server** (cổng 5500). Mở trực tiếp file `index.html` sẽ bị chặn (CORS).
- `GET /api/v1/routes`: danh sách tuyến; `GET /api/v1/routes/{id}/traffic`: giao thông trực tiếp
  (TomTom), chỉ gọi cho tuyến đang xem.
- Bản đồ dùng Leaflet + OpenStreetMap (miễn phí, không cần key).
- Nút "Find trips" mở màn hình tìm kiếm cho tuyến đó.

## Tìm chuyến & chọn ghế – dữ liệu thật
- `executeSearch()` gọi `GET /api/v1/trips/search?from=&to=&date=`; `toUiTrip()` (trong `03b-api.js`)
  đổi dữ liệu backend sang dạng các màn hình đang dùng. `generateTrips()` không còn được dùng.
- Tên tỉnh trong ô tìm kiếm ("Lam Dong (Da Lat)") được đổi sang tên thành phố của backend ("Đà Lạt")
  qua bảng `CITY_TO_PROVINCE`. Tỉnh/tuyến không có xe sẽ hiện "We don't run this route yet".
- Chọn chuyến → `GET /api/v1/trips/{id}/seats`: số hàng ghế thật, ghế đã bán, hàng 1–2 có chỗ để chân
  rộng (+30.000₫, đã cộng vào tạm tính và trang thanh toán).

## Đặt vé, mã giảm giá, My Trips – dữ liệu thật
- Giá: `POST /api/v1/bookings/quote` (giá vé + chỗ để chân rộng − mã giảm giá + 8% VAT). Trang thanh toán
  hiển thị đúng số tiền backend sẽ ghi nhận.
- Mã giảm giá kiểm tra ở backend (FIRSTRIDE 10%, WEEKEND 15% cho vé từ 300.000₫...). Các mã quảng cáo
  ở trang chủ (WEEKEND15, DALAT50K) **chưa có** trong backend.
- Thanh toán: `POST /api/v1/bookings`. Không gửi thông tin thẻ; vé được xác nhận mà chưa trừ tiền thật.
  Nếu ghế vừa bị người khác đặt (SEAT_TAKEN) → quay lại sơ đồ ghế mới.
- My Trips: mã vé + email được lưu trong trình duyệt (localStorage) và tải lại bằng
  `POST /api/v1/bookings/lookup`; hủy vé bằng `POST /api/v1/bookings/cancel`; ô "Find a booking" tìm vé khác.
- Chưa nối: đăng nhập (vẫn giả lập; khi nối, dùng `GET /api/v1/bookings/mine` cho My Trips).

## Xe buýt nội thành (City Bus) – dữ liệu OpenStreetMap
- Menu **City Bus**: 169 tuyến xe buýt TP.HCM; tìm theo số tuyến, tên đường hoặc tên trạm (gõ không dấu
  cũng được, ví dụ "ben thanh"). Chọn tuyến để xem bản đồ lộ trình, chiều đi/về, giờ chạy, tần suất,
  giá vé và danh sách trạm (bấm vào trạm để xem trên bản đồ).
- API: `GET /api/v1/city-buses?q=`, `GET /api/v1/city-buses/{id}`,
  `GET /api/v1/city-buses/stops/near?lat=&lng=` (code trong `js/screens/15c-city-buses.js`).
- Màn hình Routes hiện các tuyến xe buýt dừng gần bến xe ở TP.HCM (trong 400 m); bấm số tuyến để mở.
- Dữ liệu © OpenStreetMap contributors (ODbL) – luôn giữ dòng ghi nguồn này khi hiển thị.
