package com.ebay.behavior.gds.mdm.dec.repository;

import com.ebay.behavior.gds.mdm.dec.model.Pipeline;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PipelineRepository extends JpaRepository<Pipeline, Long> {

    List<Pipeline> findByPipelineId(String pipelineId);
}
