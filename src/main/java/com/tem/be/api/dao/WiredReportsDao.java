package com.tem.be.api.dao;

import com.tem.be.api.model.WiredReports;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface WiredReportsDao extends JpaRepository<WiredReports, Long>, JpaSpecificationExecutor<WiredReports> {
}
