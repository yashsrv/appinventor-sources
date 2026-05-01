package com.google.appinventor.client.wizards;

import static com.google.appinventor.client.Ode.MESSAGES;

import com.google.appinventor.client.ErrorReporter;
import com.google.appinventor.client.Ode;
import com.google.appinventor.client.OdeAsyncCallback;
import com.google.appinventor.client.explorer.project.Project;
import com.google.appinventor.shared.rpc.ServerLayout;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.http.client.URL;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.SimpleCheckBox;

public class ShareProjectDialog {
  interface ShareProjectDialogUiBinder extends UiBinder<Dialog, ShareProjectDialog> {}

  private static final ShareProjectDialogUiBinder UI_BINDER =
      GWT.create(ShareProjectDialogUiBinder.class);
  private String shareUrl;
  private long projectId;
  private String projectName;
  private Project project;

  @UiField Dialog shareDialog;
  @UiField SimpleCheckBox makePublicSwitch;
  @UiField FocusPanel copyLinkOption;
  @UiField FocusPanel classroomOption;
  @UiField FocusPanel msteamOption;
  @UiField Button cancelButton;

  public ShareProjectDialog() {
    UI_BINDER.createAndBindUi(this);
  }

  public void show() {
    projectId = Ode.getInstance().getCurrentYoungAndroidProjectId();
    project = Ode.getInstance().getProjectManager().getProject(projectId);
    projectName = project.getProjectName();
    makePublicSwitch.setValue(project.isProjectPublic(), false);
    shareUrl = Window.Location.getProtocol() + "//" + Window.Location.getHost()
        + "/" + ServerLayout.SHARE_PROJECT + "/" + projectId;
    shareDialog.center();
  }

  @UiHandler("makePublicSwitch")
  public void onMakePublicSwitchChange(ValueChangeEvent<Boolean> e) {
    final boolean isProjectPublic = e.getValue();
    Ode.getInstance().getProjectService().setProjectPublic(projectId, isProjectPublic,
        new OdeAsyncCallback<Void>("Unable to update project sharing setting") {
          @Override
          public void onSuccess(Void result) {
            project.setProjectPublic(isProjectPublic);
          }

          @Override
          public void onFailure(Throwable caught) {
            makePublicSwitch.setValue(project.isProjectPublic(), false);
            super.onFailure(caught);
          }
        });
  }

  private boolean isCheckboxEnabled() {
    if (makePublicSwitch.getValue()) {
      return true;
    }
    Window.alert("Turn on 'Make project importable via URL' to continue.");
    return false;
  }

  private static native void copyToClipboard(String text) /*-{
    if ($wnd.navigator && $wnd.navigator.clipboard && $wnd.navigator.clipboard.writeText) {
      $wnd.navigator.clipboard.writeText(text);
      return;
    }
    var textArea = $doc.createElement("textarea");
    textArea.value = text;
    textArea.style.position = "fixed";
    textArea.style.left = "-999999px";
    $doc.body.appendChild(textArea);
    textArea.select();
    $doc.execCommand("copy");
    $doc.body.removeChild(textArea);
  }-*/;

  @UiHandler("copyLinkOption")
  public void onCopyLinkOptionClick(@SuppressWarnings("unused") ClickEvent e) {
    if (!isCheckboxEnabled()) {
      return;
    }
    copyToClipboard(shareUrl);
    shareDialog.hide();
    ErrorReporter.reportInfo(MESSAGES.shareLinkCopied());
  }

  @UiHandler("classroomOption")
  public void onClassroomOptionClick(@SuppressWarnings("unused") ClickEvent e) {
    if (!isCheckboxEnabled()) {
      return;
    }
    String url = "https://classroom.google.com/share?url=" 
        + URL.encodeQueryString(shareUrl)
        + "&title=" + URL.encodeQueryString(projectName);

    shareDialog.hide();
    Window.open(url, "shareToClassroom", "width=600,height=600,scrollbars=yes");
  }

  @UiHandler("msteamOption")
  public void onMSTeamOptionClick(@SuppressWarnings("unused") ClickEvent e) {
    if (!isCheckboxEnabled()) {
      return;
    }

    String url = "https://teams.microsoft.com/share?href=" 
      + URL.encodeQueryString(shareUrl)
      + "&default=" + URL.encodeQueryString(projectName);

    shareDialog.hide();
    Window.open(url, "shareToTeams", "width=600,height=600,scrollbars=yes");
  }

  @UiHandler("cancelButton")
  public void close(@SuppressWarnings("unused") ClickEvent e) {
    shareDialog.hide();
  }
}
