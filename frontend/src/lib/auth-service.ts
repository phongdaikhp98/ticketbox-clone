import api from "./api";
import { ApiResponse, AuthResponse, LoginRequest, RegisterRequest, User } from "@/types/auth";

export const authService = {
  async register(data: RegisterRequest): Promise<AuthResponse> {
    const res = await api.post<ApiResponse<AuthResponse>>("/v1/auth/register", data);
    return res.data.data;
  },

  async login(data: LoginRequest): Promise<AuthResponse> {
    const res = await api.post<ApiResponse<AuthResponse>>("/v1/auth/login", data);
    return res.data.data;
  },

  async refreshToken(refreshToken: string): Promise<AuthResponse> {
    const res = await api.post<ApiResponse<AuthResponse>>("/v1/auth/refresh-token", { refreshToken });
    return res.data.data;
  },

  async getMe(): Promise<User> {
    const res = await api.get<ApiResponse<User>>("/v1/auth/me");
    return res.data.data;
  },
};
