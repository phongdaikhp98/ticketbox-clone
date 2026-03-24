import api from "./api";
import { ApiResponse, ChangePasswordRequest, UpdateProfileRequest, User } from "@/types/auth";

export const userService = {
  async updateProfile(data: UpdateProfileRequest): Promise<User> {
    const res = await api.put<ApiResponse<User>>("/v1/users/profile", data);
    return res.data.data;
  },

  async changePassword(data: ChangePasswordRequest): Promise<void> {
    await api.patch<ApiResponse<null>>("/v1/users/change-password", data);
  },
};
