import axiosClient from '../../../shared/api/axiosClient';
import type { HomeApiResponse } from '../types/homeTypes';

export const HOME_API_BASE = '/home';

export const homeApi = {
  http: axiosClient,
  basePath: HOME_API_BASE,
  getHomeData: () =>
    axiosClient.get<HomeApiResponse>(HOME_API_BASE).then((response) => response.data),
};
