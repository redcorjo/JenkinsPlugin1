package com.redcorjo.jenkins;

import com.redcorjo.shared.HttpSimpleRequest;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.*;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Sample {@link Notifier}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link RemoteHttpPublisher} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #parameters})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform} method will be invoked. 
 *
 * @author Kohsuke Kawaguchi
 */
public class RemoteHttpPublisher extends Notifier {

    private final String parameters;
    private final String headers;
    private final String user;
    private final String password;
    private final String method;
    private final String url;
    /*private final String useheaders;
    private final String useparameters;
    private final String usecredentials; */

      // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public RemoteHttpPublisher(String parameters, String headers, String url, String method, String user, String password) {

        /*
        if (useparameters != null) {
            this.parameters = useparameters;
        }
        if ( useheaders != null ) {
            this.headers = useheaders.headers;
        }
        if ( usecredentials != null ) {
            this.user = usecredentials.user;
            this.password = usecredentials.password;
        }*/
        this.parameters = parameters;
        this.method = method;
        this.url = url;
        this.user = user;
        this.password = password;
        this.headers = headers;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    /**
     * We'll use this from the {@code config.jelly}.
     */
    public String getParameters() {
        return parameters;
    }

    public String getHeaders() {return headers; }

    public String getUrl() {return url; }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getMethod() {
        return method;
    }

    /*
    public String getUseheaders() {
        return useheaders;
    }

    public String getUseparameters() {
        return useparameters;
    }

    public String getUsecredentials() {
        return usecredentials;
    }
    */

    @Override
    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener) {
        // This is where you 'build' the project.
        // Since this is a dummy, we just say 'hello world' and call that a build.

        // This also shows how you can consult the global configuration of the builder
        if (getDescriptor().getEnabled())
            listener.getLogger().println("This plugin is enabled");
        else {
            listener.getLogger().println("This plugin is disabled");
            return true;
        }

        String buildLog = build.getWorkspace() + "/" + build.number + "/log";
        listener.getLogger().println("Build log is " + buildLog);

        Jenkins jenkins = Jenkins.getInstance();

        HttpSimpleRequest myrequest = new HttpSimpleRequest(url);

        try {
            if (jenkins.proxy.name != null && jenkins.proxy.port != 0) {
                myrequest.setProxyhost(jenkins.proxy.name);
                myrequest.setProxyport(jenkins.proxy.port);
            }
        } catch (Exception e){
            listener.getLogger().println("No proxy used");
        }
        if ( headers != null) {
            myrequest.setHeaders(headers);
        }
        if ( parameters != null) {
            myrequest.setParameters(parameters);
        }
        //myrequest.setNoProxyHost(jenkins.proxy.noProxyHost);

        try {
            //myresult = myrequest.myRequest();
            myrequest.myRequest();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            listener.getLogger().println("Generic exception when launching the request");
        }
        listener.getLogger().println(myrequest.getCode());
        listener.getLogger().println(myrequest.getResult());
        listener.getLogger().println(myrequest.getResultjson());
        System.out.println("Myresult:" + myrequest.getResult());
        System.out.println("Mycode:" + myrequest.getCode());
        if ( myrequest.getCode() == 0) {
            return false;
        } else {
            return true;
        }
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link RemoteHttpPublisher}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See {@code src/main/resources/hudson/plugins/hello_world/RemoteHttpBuilder/*.jelly}
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use {@code transient}.
         */
        private boolean enabled;

        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            super(RemoteHttpPublisher.class);
            load();
        }

        /**
         * Performs on-the-fly validation of the form field 'parameters'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         *      <p>
         *      Note that returning {@link FormValidation#error(String)} does not
         *      prevent the form from being saved. It just means that a message
         *      will be displayed to the user. 
         */
        public FormValidation doCheckUrl(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.startsWith("http://") || value.startsWith("https://") )
                return FormValidation.ok();
            return FormValidation.error("Please enter an http or https URL");
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable parameters is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Remote Http Publisher";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            enabled = formData.getBoolean("enabled");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }

        /**
         * This method returns true if the global configuration says we should speak French.
         *
         * The method parameters is bit awkward because global.jelly calls this method to determine
         * the initial state of the checkbox by the naming convention.
         */
        public boolean getEnabled() {
            return enabled;
        }
    }
}

