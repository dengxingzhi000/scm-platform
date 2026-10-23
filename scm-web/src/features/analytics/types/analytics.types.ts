export type FieldType = 'METRIC' | 'DIMENSION' | 'CALCULATED'

export interface DatasetDefinition {
  id: string
  datasetCode: string
  datasetName: string
  description?: string
  datasetType: string
  sourceType: string
  sourceTable: string
  enabled: boolean
  status: number
}

export interface DatasetFieldDefinition {
  id: string
  datasetId: string
  fieldCode: string
  fieldName: string
  description?: string
  fieldType: FieldType
  dataType: string
  expr?: string
  isHidden?: boolean
  sortOrder?: number
  status?: number
}

export interface DatasetQueryFilter {
  field: string
  operator:
    | 'EQ'
    | 'NE'
    | 'GT'
    | 'GTE'
    | 'LT'
    | 'LTE'
    | 'IN'
    | 'NOT_IN'
    | 'BETWEEN'
    | 'LIKE'
    | 'NOT_LIKE'
    | 'IS_NULL'
    | 'IS_NOT_NULL'
  values: string[]
}

export interface DatasetQueryOrderBy {
  field: string
  direction: 'ASC' | 'DESC'
}

export interface DatasetQueryRequest {
  datasetCode: string
  dimensions: string[]
  metrics: string[]
  filters: DatasetQueryFilter[]
  orderBy: DatasetQueryOrderBy[]
  limit?: number
}

export interface DatasetQueryColumn {
  code: string
  dataType: string
}

export interface DatasetQueryResult {
  datasetCode: string
  columns: DatasetQueryColumn[]
  rows: Array<Record<string, unknown>>
  rowCount: number
  limit: number
  elapsedMs: number
}

export interface PageData<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}
