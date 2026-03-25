import axios from "axios";

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || "http://localhost:8083",
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use((config) => {
  const token = typeof window !== "undefined" ? localStorage.getItem("token") : null;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// [SECURITY] Token refresh + cleanup on 401 (M3).
// - Automatically retries the request with a fresh access token when the current one expires.
// - Queues concurrent requests that arrived while a refresh was in-flight.
// - On refresh failure: clears BOTH token and refreshToken before redirecting to login,
//   preventing stale refreshToken from lingering in localStorage.

let isRefreshing = false;
type QueueItem = { resolve: (token: string) => void; reject: (err: unknown) => void };
let failedQueue: QueueItem[] = [];

function processQueue(error: unknown, token: string | null) {
  failedQueue.forEach(({ resolve, reject }) =>
    error ? reject(error) : resolve(token as string)
  );
  failedQueue = [];
}

function clearAuthStorage() {
  localStorage.removeItem("token");
  // [SECURITY] Always clear refreshToken together with token — never leave it orphaned (M3)
  localStorage.removeItem("refreshToken");
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status !== 401 || originalRequest._retry) {
      return Promise.reject(error);
    }

    if (typeof window === "undefined") return Promise.reject(error);

    // Queue concurrent 401s while a refresh is in-flight
    if (isRefreshing) {
      return new Promise<string>((resolve, reject) => {
        failedQueue.push({ resolve, reject });
      }).then((newToken) => {
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return api(originalRequest);
      });
    }

    originalRequest._retry = true;
    isRefreshing = true;

    const refreshToken = localStorage.getItem("refreshToken");
    if (!refreshToken) {
      clearAuthStorage();
      window.location.href = "/login";
      isRefreshing = false;
      return Promise.reject(error);
    }

    try {
      const res = await axios.post(
        `${api.defaults.baseURL}/v1/auth/refresh-token`,
        { refreshToken }
      );
      const newAccessToken: string = res.data.data.accessToken;
      localStorage.setItem("token", newAccessToken);
      api.defaults.headers.common.Authorization = `Bearer ${newAccessToken}`;
      processQueue(null, newAccessToken);
      originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
      return api(originalRequest);
    } catch (refreshError) {
      processQueue(refreshError, null);
      clearAuthStorage();
      window.location.href = "/login";
      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  }
);

export default api;
