// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2026 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.server;

import com.google.appinventor.server.storage.StorageIo;
import com.google.appinventor.server.storage.StorageIoInstanceHolder;
import com.google.appinventor.shared.rpc.project.ProjectSourceZip;
import com.google.appinventor.shared.rpc.project.UserProject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet for importing publicly shared projects using a URL of the form /share/{projectId}.
 */
public class ShareProjectServlet extends OdeServlet {

  private final StorageIo storageIo = StorageIoInstanceHolder.getInstance();
  private final FileExporter fileExporter = new FileExporterImpl();
  private final FileImporter fileImporter = new FileImporterImpl();

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String pathInfo = req.getPathInfo();
    if (pathInfo == null || pathInfo.length() <= 1) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing project id.");
      return;
    }

    long projectId;
    try {
      projectId = Long.parseLong(pathInfo.substring(1));
    } catch (NumberFormatException e) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid project id.");
      return;
    }

    if (!storageIo.projectExists(projectId)) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Project does not exist.");
      return;
    }

    if (!storageIo.isProjectPublic(projectId)) {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Project is not public.");
      return;
    }

    String ownerUserId = storageIo.getProjectOwner(projectId);
    if (ownerUserId == null) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Project owner not found.");
      return;
    }

    String targetUserId = userInfoProvider.getUserId();
    String sourceProjectName = storageIo.getProjectName(ownerUserId, projectId);
    String importProjectName = verifyProjectName(targetUserId, sourceProjectName);

    ProjectSourceZip zipFile = fileExporter.exportProjectSourceZip(ownerUserId, projectId,
        true, false, null, false, false, false, false, false, false);

    try {
      fileImporter.importProject(targetUserId, importProjectName,
          new ByteArrayInputStream(zipFile.getContent()));
      resp.sendRedirect("/");
    } catch (FileImporterException e) {
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Unable to import shared project.");
    }
  }

  private String verifyProjectName(String userId, String projectName) {
    String normalized = projectName.replace(" ", "_");
    int count = 0;
    List<Long> projectIds = storageIo.getProjects(userId);
    List<UserProject> projects = storageIo.getUserProjects(userId, projectIds);
    TreeSet<String> projectNames = new TreeSet<String>();
    for (UserProject project : projects) {
      projectNames.add(project.getProjectName());
    }

    String candidate = normalized;
    while (projectNames.contains(candidate)) {
      count++;
      if (count > 100) {
        throw new IllegalStateException("Unable to generate unique project name");
      }
      candidate = normalized + "_" + count;
    }
    return candidate;
  }
}
