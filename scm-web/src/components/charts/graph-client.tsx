"use client";

import { useEffect, useRef } from "react";
import { Graph } from "@antv/g6";

import { cn } from "@/lib/utils";

export interface TopologyNode {
  id: string;
  label?: string;
}

export interface TopologyEdge {
  source: string;
  target: string;
  label?: string;
}

export interface TopologyData {
  nodes: TopologyNode[];
  edges: TopologyEdge[];
}

interface GraphClientProps {
  data: TopologyData;
  height?: number;
  className?: string;
  layout?: "force" | "antv-dagre" | "circular" | "grid" | "radial";
}

/**
 * G6 topology renderer. Always loaded through `@/components/charts/topology-graph`
 * (dynamic + `ssr: false`).
 */
export default function GraphClient({
  data,
  height = 360,
  className,
  layout = "force",
}: GraphClientProps) {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const graph = new Graph({
      container,
      autoFit: "view",
      data: {
        nodes: data.nodes.map((node) => ({
          id: node.id,
          data: { label: node.label ?? node.id },
        })),
        edges: data.edges.map((edge) => ({
          source: edge.source,
          target: edge.target,
          data: { label: edge.label },
        })),
      },
      layout: { type: layout, animation: false },
      node: {
        style: {
          size: 34,
          fill: "var(--color-primary)",
          labelText: (datum) =>
            String(datum.data?.label ?? datum.id ?? ""),
          labelFill: "var(--color-primary-on)",
          fontSize: 11,
        },
      },
      edge: {
        style: {
          stroke: "var(--color-border)",
          endArrow: true,
        },
      },
      behaviors: ["drag-canvas", "zoom-canvas", "drag-element"],
    });

    void graph.render();

    const observer = new ResizeObserver(() => {
      void graph.resize();
    });
    observer.observe(container);

    return () => {
      observer.disconnect();
      graph.destroy();
    };
  }, [data, layout]);

  return (
    <div
      ref={containerRef}
      className={cn("w-full", className)}
      style={{ height }}
    />
  );
}
