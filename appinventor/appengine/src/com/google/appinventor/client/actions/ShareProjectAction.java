package com.google.appinventor.client.actions;

import com.google.appinventor.client.wizards.ShareProjectDialog;
import com.google.gwt.user.client.Command;

public class ShareProjectAction implements Command {
  @Override
  public void execute() {
    new ShareProjectDialog().show();
  }

}