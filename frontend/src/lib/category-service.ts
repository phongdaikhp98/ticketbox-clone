import api from "./api";
import { ApiResponse } from "@/types/auth";
import { CategoryInfo } from "@/types/event";

export interface CategoryRequest {
  name: string;
  slug?: string;
  icon?: string;
  displayOrder?: number;
}

export const categoryService = {
  async getCategories(): Promise<CategoryInfo[]> {
    const res = await api.get<ApiResponse<CategoryInfo[]>>("/v1/categories");
    return res.data.data;
  },

  async createCategory(data: CategoryRequest): Promise<CategoryInfo> {
    const res = await api.post<ApiResponse<CategoryInfo>>(
      "/v1/admin/categories",
      data
    );
    return res.data.data;
  },

  async updateCategory(id: number, data: CategoryRequest): Promise<CategoryInfo> {
    const res = await api.put<ApiResponse<CategoryInfo>>(
      `/v1/admin/categories/${id}`,
      data
    );
    return res.data.data;
  },

  async deleteCategory(id: number): Promise<void> {
    await api.delete(`/v1/admin/categories/${id}`);
  },
};
