import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Ticketbox - Mua vé sự kiện trực tuyến",
  description: "Mua vé hòa nhạc, hội thảo, thể thao, phim, kịch và voucher",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="vi">
      <body>{children}</body>
    </html>
  );
}
