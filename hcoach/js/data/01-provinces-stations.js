/* =========================================================================
 * 1. ALL 63 PROVINCES & MUNICIPALITIES OF VIETNAM + REAL BUS STATIONS
 * ========================================================================= */
const ALL_PROVINCES_VN = [
  'An Giang', 'Ba Ria - Vung Tau', 'Bac Giang', 'Bac Kan', 'Bac Lieu',
  'Bac Ninh', 'Ben Tre', 'Binh Dinh (Quy Nhon)', 'Binh Duong', 'Binh Phuoc',
  'Binh Thuan (Mui Ne)', 'Ca Mau', 'Can Tho', 'Cao Bang', 'Da Nang',
  'Dak Lak (Buon Ma Thuot)', 'Dak Nong', 'Dien Bien', 'Dong Nai', 'Dong Thap',
  'Gia Lai (Pleiku)', 'Ha Giang', 'Ha Nam', 'Ha Tinh', 'Hai Duong',
  'Hai Phong', 'Hanoi', 'Hau Giang', 'Ho Chi Minh City', 'Hoa Binh',
  'Hung Yen', 'Khanh Hoa (Nha Trang)', 'Kien Giang (Phu Quoc)', 'Kon Tum', 'Lai Chau',
  'Lam Dong (Da Lat)', 'Lang Son', 'Lao Cai (Sa Pa)', 'Long An', 'Nam Dinh',
  'Nghe An (Vinh)', 'Ninh Binh', 'Ninh Thuan (Phan Rang)', 'Phu Tho', 'Phu Yen (Tuy Hoa)',
  'Quang Binh (Dong Hoi)', 'Quang Nam (Hoi An)', 'Quang Ngai', 'Quang Ninh (Ha Long)', 'Quang Tri',
  'Soc Trang', 'Son La', 'Tay Ninh', 'Thai Binh', 'Thai Nguyen',
  'Thanh Hoa', 'Thua Thien Hue', 'Tien Giang', 'Tra Vinh', 'Tuyen Quang',
  'Vinh Long', 'Vinh Phuc', 'Yen Bai'
];

const FEATURED_CITIES = [
  'Ho Chi Minh City', 'Hanoi', 'Da Nang', 'Lam Dong (Da Lat)',
  'Lao Cai (Sa Pa)', 'Khanh Hoa (Nha Trang)', 'Can Tho',
  'Hai Phong', 'Thua Thien Hue', 'Quang Ninh (Ha Long)', 'Ba Ria - Vung Tau'
];

// Extra search aliases so both Vietnamese (with/without tones) & English queries match
const CITY_SEARCH_ALIASES = {
  'Ho Chi Minh City': 'sai gon saigon hcm tphcm tp ho chi minh',
  'Hanoi': 'ha noi hn',
  'Da Nang': 'da nang danang',
  'Lam Dong (Da Lat)': 'da lat dalat lam dong',
  'Lao Cai (Sa Pa)': 'sapa sa pa lao cai',
  'Khanh Hoa (Nha Trang)': 'nha trang khanh hoa',
  'Thua Thien Hue': 'hue thua thien hue',
  'Quang Ninh (Ha Long)': 'ha long halong quang ninh',
  'Kien Giang (Phu Quoc)': 'phu quoc rach gia kien giang',
  'Quang Nam (Hoi An)': 'hoi an quang nam',
  'Binh Thuan (Mui Ne)': 'mui ne phan thiet binh thuan'
};

const BUS_STATIONS_MAP = {
  'Ho Chi Minh City': 'New Mien Dong Bus Station',
  'Hanoi': 'My Dinh Bus Station',
  'Da Nang': 'Da Nang Central Bus Station',
  'Lam Dong (Da Lat)': 'Da Lat Intercity Bus Station',
  'Lao Cai (Sa Pa)': 'Sa Pa Bus Station',
  'Khanh Hoa (Nha Trang)': 'South Nha Trang Bus Station',
  'Can Tho': 'Can Tho Central Bus Station',
  'Hai Phong': 'Vinh Niem Bus Station',
  'Thua Thien Hue': 'South Hue Bus Station',
  'Ba Ria - Vung Tau': 'Vung Tau Bus Station',
  'Quang Ninh (Ha Long)': 'Bai Chay Bus Station',
  'Quang Nam (Hoi An)': 'Hoi An Bus Station',
  'Kien Giang (Phu Quoc)': 'Rach Gia Bus Station',
  'Nghe An (Vinh)': 'North Vinh Bus Station',
  'Thanh Hoa': 'North Thanh Hoa Bus Station',
  'Binh Dinh (Quy Nhon)': 'Quy Nhon Bus Station',
  'Dak Lak (Buon Ma Thuot)': 'South Buon Ma Thuot Bus Station',
  'Gia Lai (Pleiku)': 'Duc Long Gia Lai Bus Station',
  'Binh Thuan (Mui Ne)': 'North Phan Thiet Bus Station',
  'Ninh Binh': 'Ninh Binh Central Bus Station',
  'Dong Nai': 'Bien Hoa Bus Station',
  'Binh Duong': 'Binh Duong Bus Station',
  'An Giang': 'Long Xuyen Bus Station',
  'Ca Mau': 'Ca Mau Bus Station'
};

function getStationName(province) {
  if (BUS_STATIONS_MAP[province]) return BUS_STATIONS_MAP[province];
  const clean = province.replace(/\s*\(.*?\)\s*/g, '').trim();
  return `${clean} Central Bus Station`;
}

function normalizeSearch(str) {
  return String(str || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
    .trim();
}

function formatVND(amount) {
  return Number(amount || 0).toLocaleString('en-US') + '₫';
}

function getDefaultDate() {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function formatDateEN(isoDate) {
  if (!isoDate) return 'Tomorrow';
  const parts = isoDate.split('-');
  if (parts.length !== 3) return isoDate;
  const d = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
  const days = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
  const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  return `${days[d.getDay()]}, ${months[d.getMonth()]} ${d.getDate()}, ${d.getFullYear()}`;
}

function escapeHtml(str) {
  return String(str ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
