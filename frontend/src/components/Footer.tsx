"use client";

export default function Footer() {
  return (
    <footer>
      {/* Top section — Info + Links */}
      <div className="bg-[#2D3748] border-t border-zinc-600">
        <div className="max-w-7xl mx-auto px-4 py-10 grid grid-cols-1 md:grid-cols-3 gap-8">
          {/* Column 1: Contact */}
          <div>
            <h4 className="text-white font-bold mb-4">Hotline</h4>
            <p className="text-gray-400 text-sm mb-1 flex items-center gap-2">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
              </svg>
              Thứ 2 - Chủ Nhật (8:00 - 23:00)
            </p>
            <p className="text-primary text-xl font-bold mb-4">1900.6408</p>

            <h4 className="text-white font-bold mb-2">Email</h4>
            <p className="text-gray-400 text-sm flex items-center gap-2 mb-4">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
              </svg>
              support@ticketbox.vn
            </p>

            <h4 className="text-white font-bold mb-2">Văn phòng chính</h4>
            <p className="text-gray-400 text-sm flex items-start gap-2">
              <svg className="w-4 h-4 mt-0.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
              Tầng 12, Tòa nhà Viettel, 285 Cách Mạng Tháng Tám,
              Phường Hòa Hưng, TP. Hồ Chí Minh
            </p>
          </div>

          {/* Column 2: Customer & Organizer */}
          <div>
            <h4 className="text-white font-bold mb-4">Dành cho Khách hàng</h4>
            <ul className="space-y-2 mb-6">
              <li>
                <a href="#" className="text-gray-400 text-sm hover:text-primary transition">
                  Điều khoản sử dụng cho khách hàng
                </a>
              </li>
            </ul>

            <h4 className="text-white font-bold mb-4">Dành cho Ban Tổ chức</h4>
            <ul className="space-y-2">
              <li>
                <a href="#" className="text-gray-400 text-sm hover:text-primary transition">
                  Điều khoản sử dụng cho ban tổ chức
                </a>
              </li>
            </ul>
          </div>

          {/* Column 3: Company */}
          <div>
            <h4 className="text-white font-bold italic mb-4">Về công ty chúng tôi</h4>
            <ul className="space-y-2">
              <li>
                <a href="#" className="text-gray-400 text-sm hover:text-primary transition">
                  Quy chế hoạt động
                </a>
              </li>
              <li>
                <a href="#" className="text-gray-400 text-sm hover:text-primary transition">
                  Chính sách bảo mật thông tin
                </a>
              </li>
              <li>
                <a href="#" className="text-gray-400 text-sm hover:text-primary transition">
                  Cơ chế giải quyết tranh chấp/ khiếu nại
                </a>
              </li>
              <li>
                <a href="#" className="text-gray-400 text-sm hover:text-primary transition">
                  Chính sách bảo mật thanh toán
                </a>
              </li>
              <li>
                <a href="#" className="text-gray-400 text-sm hover:text-primary transition">
                  Chính sách đổi trả và kiểm hàng
                </a>
              </li>
              <li>
                <a href="#" className="text-gray-400 text-sm hover:text-primary transition">
                  Điều kiện vận chuyển và giao nhận
                </a>
              </li>
              <li>
                <a href="#" className="text-gray-400 text-sm hover:text-primary transition">
                  Phương thức thanh toán
                </a>
              </li>
            </ul>
          </div>
        </div>
      </div>

      {/* Bottom section — Logo + Company info */}
      <div className="bg-[#1A1A2E]">
        <div className="max-w-7xl mx-auto px-4 py-8 flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
          {/* Logo + tagline */}
          <div className="flex-shrink-0">
            <div className="mb-2">
              <span className="text-white text-3xl font-bold tracking-tight">ticketbox</span>
              <div className="text-gray-400 text-xs mt-1">
                by <span className="text-primary font-semibold">PhongNH</span>
              </div>
            </div>
            <p className="text-gray-500 text-sm italic mt-3 leading-relaxed">
              Nền tảng quản lý và phân phối vé sự kiện hàng đầu<br />
              Việt Nam
            </p>
            <p className="text-gray-600 text-xs mt-2">&copy; 2017</p>
          </div>

          {/* Company info */}
          <div className="text-gray-400 text-sm leading-relaxed">
            <p className="font-semibold text-gray-300 mb-1">Công ty TNHH Ticketbox</p>
            <p>Đại diện theo pháp luật: Phạm Thị Hường</p>
            <p className="mt-1">
              Giấy chứng nhận đăng ký doanh nghiệp số: 0313605444, cấp lần đầu ngày<br />
              07/01/2016 bởi Sở Kế Hoạch và Đầu Tư TP. Hồ Chí Minh
            </p>
          </div>

          {/* Badge */}
          <div className="flex-shrink-0">
            <div className="w-32 h-16 bg-red-600 rounded-lg flex flex-col items-center justify-center">
              <span className="text-white text-xs font-bold uppercase leading-tight text-center">
                ĐÃ ĐĂNG KÝ
              </span>
              <span className="text-white text-[10px] mt-0.5">BỘ CÔNG THƯƠNG</span>
            </div>
          </div>
        </div>
      </div>
    </footer>
  );
}
