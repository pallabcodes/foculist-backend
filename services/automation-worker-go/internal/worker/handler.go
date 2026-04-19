package worker

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
)

// Job represents a background task payload
type Job struct {
	ID        string          `json:"id"`
	Type      string          `json:"type"`
	TenantID  string          `json:"tenantId"`
	Payload   json.RawMessage `json:"payload"`
}

// JobHandler handles specific job types
type JobHandler interface {
	Handle(ctx context.Context, job Job) error
}

// Registry maps job types to handlers
type Registry struct {
	handlers map[string]JobHandler
}

func NewRegistry() *Registry {
	return &Registry{
		handlers: make(map[string]JobHandler),
	}
}

func (r *Registry) Register(jobType string, handler JobHandler) {
	r.handlers[jobType] = handler
}

func (r *Registry) Get(jobType string) (JobHandler, bool) {
	h, ok := r.handlers[jobType]
	return h, ok
}

// TaskEnrichmentHandler simulates AI task enrichment
type TaskEnrichmentHandler struct{}

func (h *TaskEnrichmentHandler) Handle(ctx context.Context, job Job) error {
	log.Printf("[TaskEnrichment] Processing job %s for tenant %s", job.ID, job.TenantID)
	// Mock AI logic: parse original task and add AI-generated subtasks or metadata
	fmt.Printf("Enriching task: %s\n", string(job.Payload))
	return nil
}
