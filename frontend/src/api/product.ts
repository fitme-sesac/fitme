import { http } from "./http";

export interface Product {
    productId: number;
    productCode: string;
    productType: string;
    name: string;
    priceAmount: number;
    currency: string;
    creditAmount: number;
    createdAt: string;
    updatedAt: string;
}

interface ProductPageResponse {
    content: Product[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
}

export const fetchProducts = async (type?: string): Promise<ProductPageResponse> => {
    const params = type ? { type } : {};
    const response = await http.get<ProductPageResponse>("/api/v1/products", { params });
    return response.data;
};
