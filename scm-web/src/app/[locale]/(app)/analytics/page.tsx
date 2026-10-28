'use client'

import { useMemo, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Col,
  Empty,
  InputNumber,
  Row,
  Select,
  Space,
  Typography,
  message,
} from 'antd'
import { PlayCircleOutlined, ReloadOutlined } from '@ant-design/icons'
import AnalyticsChart from '@/features/analytics/components/analytics-chart'
import AnalyticsResultTable from '@/features/analytics/components/analytics-result-table'
import { useAnalyticsQuery, useDatasetFields, useDatasets } from '@/features/analytics/hooks'
import { PageHeader } from '@/components/ui/page-header'

const { Text } = Typography

export default function AnalyticsPage() {
  const datasetsQuery = useDatasets()
  const [datasetCode, setDatasetCode] = useState<string>()
  const [dimensions, setDimensions] = useState<string[]>([])
  const [metrics, setMetrics] = useState<string[]>([])
  const [selectionDatasetId, setSelectionDatasetId] = useState<string>()
  const [limit, setLimit] = useState(1000)
  const queryMutation = useAnalyticsQuery()

  const datasets = useMemo(
    () => datasetsQuery.data?.records ?? [],
    [datasetsQuery.data],
  )
  const activeDatasetCode = datasetCode ?? datasets[0]?.datasetCode
  const selectedDataset = useMemo(
    () => datasets.find((dataset) => dataset.datasetCode === activeDatasetCode),
    [datasets, activeDatasetCode],
  )
  const fieldsQuery = useDatasetFields(selectedDataset?.id)
  const fields = useMemo(() => fieldsQuery.data ?? [], [fieldsQuery.data])

  const dimensionOptions = useMemo(
    () =>
      fields
        .filter((field) => !field.isHidden && field.fieldType !== 'METRIC')
        .map((field) => ({ label: `${field.fieldName} (${field.fieldCode})`, value: field.fieldCode })),
    [fields],
  )
  const metricOptions = useMemo(
    () =>
      fields
        .filter((field) => !field.isHidden && field.fieldType === 'METRIC')
        .map((field) => ({ label: `${field.fieldName} (${field.fieldCode})`, value: field.fieldCode })),
    [fields],
  )

  const hasExplicitSelection = selectionDatasetId === selectedDataset?.id
  const effectiveDimensions = hasExplicitSelection
    ? dimensions
    : dimensionOptions[0]
      ? [dimensionOptions[0].value]
      : []
  const effectiveMetrics = hasExplicitSelection
    ? metrics
    : metricOptions[0]
      ? [metricOptions[0].value]
      : []

  const handleDatasetChange = (code: string) => {
    setDatasetCode(code)
    setSelectionDatasetId(undefined)
    setDimensions([])
    setMetrics([])
    queryMutation.reset()
  }

  const handleDimensionsChange = (values: string[]) => {
    setSelectionDatasetId(selectedDataset?.id)
    setDimensions(values)
  }

  const handleMetricsChange = (values: string[]) => {
    setSelectionDatasetId(selectedDataset?.id)
    setMetrics(values)
  }

  const runQuery = () => {
    if (!selectedDataset) {
      message.warning('请先选择数据集')
      return
    }
    if (effectiveDimensions.length === 0 && effectiveMetrics.length === 0) {
      message.warning('至少选择一个维度或指标')
      return
    }
    queryMutation.mutate({
      datasetCode: selectedDataset.datasetCode,
      dimensions: effectiveDimensions,
      metrics: effectiveMetrics,
      filters: [],
      orderBy: [],
      limit,
    })
  }

  const refreshMetadata = () => {
    datasetsQuery.refetch()
    fieldsQuery.refetch()
  }

  const error = datasetsQuery.error ?? fieldsQuery.error ?? queryMutation.error

  return (
    <Space orientation="vertical" size={16} style={{ width: '100%' }}>
      <PageHeader
        eyebrow="数据洞察"
        title="数据分析"
        description="选择数据集、维度与指标，运行即席查询并导出结果"
      />

      <Card>
        <Row gutter={[16, 16]} align="bottom">
          <Col xs={24} lg={8}>
            <Text type="secondary">数据集</Text>
            <Select
              style={{ width: '100%', marginTop: 4 }}
              placeholder="选择数据集"
              loading={datasetsQuery.isLoading}
              value={activeDatasetCode}
              onChange={handleDatasetChange}
              options={datasets.map((dataset) => ({
                label: `${dataset.datasetName} (${dataset.datasetCode})`,
                value: dataset.datasetCode,
                disabled: !dataset.enabled,
              }))}
            />
          </Col>
          <Col xs={24} md={12} lg={6}>
            <Text type="secondary">维度</Text>
            <Select
              mode="multiple"
              style={{ width: '100%', marginTop: 4 }}
              placeholder="选择维度"
              loading={fieldsQuery.isLoading}
              value={effectiveDimensions}
              onChange={handleDimensionsChange}
              options={dimensionOptions}
              maxTagCount="responsive"
            />
          </Col>
          <Col xs={24} md={12} lg={6}>
            <Text type="secondary">指标</Text>
            <Select
              mode="multiple"
              style={{ width: '100%', marginTop: 4 }}
              placeholder="选择指标"
              loading={fieldsQuery.isLoading}
              value={effectiveMetrics}
              onChange={handleMetricsChange}
              options={metricOptions}
              maxTagCount="responsive"
            />
          </Col>
          <Col xs={12} md={6} lg={2}>
            <Text type="secondary">行数上限</Text>
            <InputNumber
              style={{ width: '100%', marginTop: 4 }}
              min={1}
              max={10000}
              step={100}
              value={limit}
              onChange={(value) => setLimit(value ?? 1000)}
            />
          </Col>
          <Col xs={12} md={6} lg={2}>
            <Space>
              <Button
                type="primary"
                icon={<PlayCircleOutlined />}
                loading={queryMutation.isPending}
                onClick={runQuery}
              >
                运行
              </Button>
              <Button icon={<ReloadOutlined />} onClick={refreshMetadata}>
                刷新
              </Button>
            </Space>
          </Col>
        </Row>
        {error && (
          <Alert
            style={{ marginTop: 16 }}
            type="error"
            showIcon
            title="查询失败"
            description={error.message}
          />
        )}
      </Card>

      {queryMutation.data ? (
        <>
          <AnalyticsChart
            result={queryMutation.data}
            dimensions={effectiveDimensions}
            metrics={effectiveMetrics}
            loading={queryMutation.isPending}
          />
          <AnalyticsResultTable result={queryMutation.data} />
        </>
      ) : (
        <Card>
          <Empty description="选择数据集与字段后运行查询" />
        </Card>
      )}
    </Space>
  )
}
