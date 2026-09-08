export interface Drug {
  id?: number
  code?: string
  /** 药品 / 器械，对应编号 3xxxxx / 4xxxxx */
  category?: string
  genericName: string
  tradeName?: string
  spec?: string
  dosageForm?: string
  unit?: string
  manufacturer?: string
  approvalNo?: string
  barcode?: string
  isColdChain?: number
  rxType?: string
  refPurchasePrice?: number
  refSalePrice?: number
  status?: number
  remark?: string
}
