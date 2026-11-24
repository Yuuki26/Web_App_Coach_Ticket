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
js/
  data/
    01-provinces-stations.js    63 tỉnh, bến xe, hàm định dạng (formatVND, ngày...)
    02-routes-promos-reviews.js Tuyến phổ biến, mã giảm giá, deal đếm ngược, đánh giá, nhà xe
  core/
    03-state.js                 Biến `state` – toàn bộ trạng thái ứng dụng
    04-icons-illustrations.js   Icon SVG, hình xe, ảnh địa danh, mã QR
    05-toast-clipboard.js       Thông báo toast & copy
    06-trip-seat-generator.js   Sinh chuyến xe & sơ đồ ghế giả lập
  components/
    07-header-stepper-auth.js   Header, stepper, modal đăng nhập
    08-city-picker.js           Dropdown chọn tỉnh/thành
  screens/
    09-home.js … 15-my-trips.js Mỗi file = 1 màn hình (render HTML)
  16-actions.js                 Xử lý sự kiện (onclick: chọn ghế, thanh toán...)
  17-main.js                    Hàm render() chính & khởi động app
```

## Lưu ý quan trọng
- Các file JS là script thường, dùng chung biến toàn cục (`state`, các hàm render...).
  **Không đổi thứ tự** thẻ `<script>` trong `index.html` và **không** thêm `type="module"`,
  nếu không các nút `onclick="..."` sẽ không tìm thấy hàm.
- Luồng chạy: người dùng bấm → hàm trong `16-actions.js` sửa `state` → gọi `render()`
  (`17-main.js`) → vẽ lại màn hình tương ứng trong `screens/`.
