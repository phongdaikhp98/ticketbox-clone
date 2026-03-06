import type { Metadata } from "next";
import "./globals.css";
import { AuthProvider } from "@/contexts/AuthContext";
import { CartProvider } from "@/contexts/CartContext";
import Footer from "@/components/Footer";

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
      <body className="flex flex-col min-h-screen">
        <AuthProvider>
          <CartProvider>
            <div className="flex-1">{children}</div>
            <Footer />
          </CartProvider>
        </AuthProvider>
      </body>
    </html>
  );
}
