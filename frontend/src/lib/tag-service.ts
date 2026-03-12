import api from "./api";
import { ApiResponse } from "@/types/auth";
import { TagInfo } from "@/types/event";

export const tagService = {
  async getAllTags(): Promise<TagInfo[]> {
    const res = await api.get<ApiResponse<TagInfo[]>>("/v1/tags");
    return res.data.data;
  },

  async getPopularTags(): Promise<TagInfo[]> {
    const res = await api.get<ApiResponse<TagInfo[]>>("/v1/tags/popular");
    return res.data.data;
  },
};
