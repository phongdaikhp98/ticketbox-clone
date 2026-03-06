"use client";

import { Suspense, useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import Header from "@/components/Header";
import { orderService } from "@/lib/order-service";

function VNPayReturnContent() {
  const searchParams = useSearchParams();
  const [status, setStatus] = useState<"loading" | "success" | "failed">("loading");
  const [orderId, setOrderId] = useState<string | null>(null);

  useEffect(() => {
    const params: Record<string, string> = {};
    searchParams.forEach((value, key) => {
      params[key] = value;
    });

    const txnRef = params["vnp_TxnRef"];
    if (txnRef) {
      setOrderId(txnRef.split("_")[0]);
    }

    orderService.verifyVnPayReturn(params)
      .then((result) => {
        setStatus(result.RspCode === "00" ? "success" : "failed");
      })
      .catch(() => {
        const responseCode = params["vnp_ResponseCode"];
        setStatus(responseCode === "00" ? "success" : "failed");
      });
  }, [searchParams]);

  if (status === "loading") {
    return <div className="text-center text-gray-400">Processing...</div>;
  }

  if (status === "success") {
    return (
      <div className="text-center space-y-6">
        <div className="w-20 h-20 bg-green-500/20 rounded-full flex items-center justify-center mx-auto">
          <svg className="w-10 h-10 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
          </svg>
        </div>
        <h1 className="text-2xl font-bold text-white">Thanh toán thành công!</h1>
        <p className="text-gray-400">
          Đơn hàng của bạn đã được thanh toán thành công qua VNPay.
        </p>
        {orderId && (
          <Link
            href={`/orders/${orderId}`}
            className="inline-block px-6 py-3 bg-primary text-white rounded-lg hover:bg-green-600 transition font-medium"
          >
            Xem chi tiết đơn hàng
          </Link>
        )}
      </div>
    );
  }

  return (
    <div className="text-center space-y-6">
      <div className="w-20 h-20 bg-red-500/20 rounded-full flex items-center justify-center mx-auto">
        <svg className="w-10 h-10 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
        </svg>
      </div>
      <h1 className="text-2xl font-bold text-white">Thanh toán thất bại</h1>
      <p className="text-gray-400">
        Giao dịch không thành công. Vui lòng thử lại.
      </p>
      <div className="flex gap-3 justify-center">
        {orderId && (
          <Link
            href={`/orders/${orderId}`}
            className="px-6 py-3 bg-zinc-700 text-white rounded-lg hover:bg-zinc-600 transition font-medium"
          >
            Xem đơn hàng
          </Link>
        )}
        <Link
          href="/orders"
          className="px-6 py-3 bg-primary text-white rounded-lg hover:bg-green-600 transition font-medium"
        >
          Đơn hàng của tôi
        </Link>
      </div>
    </div>
  );
}

export default function VNPayReturnPage() {
  return (
    <div className="min-h-screen bg-secondary">
      <Header />
      <main className="max-w-lg mx-auto px-4 py-16">
        <Suspense fallback={<div className="text-center text-gray-400">Processing...</div>}>
          <VNPayReturnContent />
        </Suspense>
      </main>
    </div>
  );
}
