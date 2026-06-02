/**
 *
 * (c) Copyright Ascensio System SIA 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import axios from "axios";
import axiosRetry from "axios-retry";

import {
  DealsResponse,
  DealSearchResponse,
  DealFilesResponse,
} from "src/types/deal";
import { PipedriveToken } from "@context/PipedriveToken";

export const getDeals = async (
  pipedriveToken: PipedriveToken,
  params?: { limit?: number; cursor?: string },
) => {
  const token = await pipedriveToken.getValue();
  const client = axios.create({ baseURL: process.env.BACKEND_URL });
  axiosRetry(client, {
    retries: 2,
    retryCondition: (error) =>
      axiosRetry.isNetworkOrIdempotentRequestError(error) ||
      (error.response?.status !== undefined && error.response.status >= 500),
    retryDelay: (count) => count * 50,
    shouldResetTimeout: true,
  });

  const response = await client<DealsResponse>({
    method: "GET",
    url: "/api/v1/deals",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    params,
    timeout: 10000,
  });

  return response.data;
};

export const searchDeals = async (
  pipedriveToken: PipedriveToken,
  params?: { term?: string; limit?: number; cursor?: string },
) => {
  const token = await pipedriveToken.getValue();
  const client = axios.create({ baseURL: process.env.BACKEND_URL });
  axiosRetry(client, {
    retries: 2,
    retryCondition: (error) =>
      axiosRetry.isNetworkOrIdempotentRequestError(error) ||
      (error.response?.status !== undefined && error.response.status >= 500),
    retryDelay: (count) => count * 50,
    shouldResetTimeout: true,
  });

  const response = await client<DealSearchResponse>({
    method: "GET",
    url: "/api/v1/deals/search",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    params,
    timeout: 10000,
  });

  return response.data;
};

export const getDealFiles = async (
  pipedriveToken: PipedriveToken,
  dealId: number,
  params?: { start?: number; limit?: number },
) => {
  const token = await pipedriveToken.getValue();
  const client = axios.create({ baseURL: process.env.BACKEND_URL });
  axiosRetry(client, {
    retries: 2,
    retryCondition: (error) =>
      axiosRetry.isNetworkOrIdempotentRequestError(error) ||
      (error.response?.status !== undefined && error.response.status >= 500),
    retryDelay: (count) => count * 50,
    shouldResetTimeout: true,
  });

  const response = await client<DealFilesResponse>({
    method: "GET",
    url: `/api/v1/deals/${dealId}/files`,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    params,
    timeout: 10000,
  });

  return response.data;
};
