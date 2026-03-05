import api from "./api";
import { ApiResponse, UpdateProfileRequest, User } from "@/types/auth";

export const userService = {
  async updateProfile(data: UpdateProfileRequest): Promise<User> {
    const res = await api.put<ApiResponse<User>>("/v1/users/profile", data);
    return res.data.data;
  },
};
