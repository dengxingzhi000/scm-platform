import { systemService } from "@/lib/api/services/system";
import {
  toDictItem,
  toDictItemPayload,
  toDictType,
  toDictTypePayload,
} from "../lib/mappers";
import type { DictItem, DictType } from "../types";

export const dictService = {
  listTypes: async (): Promise<DictType[]> =>
    (await systemService.dicts.listTypes()).map(toDictType),

  getTypeById: async (id: string): Promise<DictType> => {
    const types = await systemService.dicts.listTypes();
    const type = types.find((item) => item.id === id);
    if (!type) throw new Error("Dict type not found");
    return toDictType(type);
  },

  createType: async (data: Partial<DictType>): Promise<void> => {
    await systemService.dicts.createType(toDictTypePayload(data));
  },

  updateType: async (id: string, data: Partial<DictType>): Promise<void> => {
    await systemService.dicts.updateType(
      toDictTypePayload({ ...data, id })
    );
  },

  deleteType: async (id: string): Promise<void> => {
    await systemService.dicts.deleteType(id);
  },

  listItems: async (dictCode: string): Promise<DictItem[]> =>
    (await systemService.dicts.listItems(dictCode)).map(toDictItem),

  createItem: async (
    data: Partial<DictItem>,
    dictTypeId: string,
    dictCode: string
  ): Promise<void> => {
    await systemService.dicts.createItem(
      toDictItemPayload(data, dictTypeId, dictCode)
    );
  },

  updateItem: async (
    data: Partial<DictItem>,
    dictTypeId: string,
    dictCode: string
  ): Promise<void> => {
    await systemService.dicts.updateItem(
      toDictItemPayload(data, dictTypeId, dictCode)
    );
  },

  deleteItem: async (id: string): Promise<void> => {
    await systemService.dicts.deleteItem(id);
  },
};
