export interface Settlement {
  id: string
  settlementNo: string
  supplierName: string
  amount: number
  status: 'pending' | 'processing' | 'completed' | 'rejected'
  statusLabel: string
  period: string
  createdAt: string
}

export interface Invoice {
  id: string
  invoiceNo: string
  settlementNo: string
  amount: number
  taxAmount: number
  status: 'pending' | 'issued' | 'sent' | 'received' | 'voided' | 'red_flushed'
  statusLabel: string
  createdAt: string
}
