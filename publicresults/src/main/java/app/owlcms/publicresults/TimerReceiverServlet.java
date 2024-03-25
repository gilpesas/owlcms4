/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.publicresults;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

import app.owlcms.uievents.BreakTimerEvent;
import app.owlcms.uievents.TimerEvent;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ProxyUtils;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/timer")
public class TimerReceiverServlet extends HttpServlet {

    private static String defaultFopName;
    static EventBus eventBus = new AsyncEventBus(TimerReceiverServlet.class.getSimpleName(),
            Executors.newCachedThreadPool());

    public static EventBus getEventBus() {
        return eventBus;
    }

    Logger logger = (Logger) LoggerFactory.getLogger(TimerReceiverServlet.class);

    private String secret = StartupUtils.getStringParam("updateKey");

    /**
     * @see jakarta.servlet.http.HttpServlet#doGet(jakarta.servlet.http.HttpServletRequest,
     *      jakarta.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // get makes no sense on this URL. Standard says there shouldn't be a 405 on a
        // get,
        // but "disallowed" is what makes most sense as a return code.
        resp.sendError(405);
    }

    /**
     * @see jakarta.servlet.http.HttpServlet#doPost(jakarta.servlet.http.HttpServletRequest,
     *      jakarta.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        try {
            resp.setCharacterEncoding("UTF-8");
            if (StartupUtils.isTraceSetting()) {
                Set<Entry<String, String[]>> pairs = req.getParameterMap().entrySet();
                this.logger./**/warn("---- timer update received from {}", ProxyUtils.getClientIp(req));
                for (Entry<String, String[]> pair : pairs) {
                    this.logger./**/warn("    {} = {}", pair.getKey(), pair.getValue()[0]);
                }
            }

            String updateKey = req.getParameter("updateKey");
            if (updateKey == null || !updateKey.equals(this.secret)) {
                this.logger.error("denying access from {} expected {} got {} ", req.getRemoteHost(), this.secret,
                        updateKey);
                resp.sendError(401, "Denied, wrong credentials");
                return;
            }

            TimerEvent timerEvent = null;
            BreakTimerEvent breakTimerEvent = null;

            String eventTypeString = req.getParameter("eventType");
            String fopName = req.getParameter("fopName");

            String secondsString = req.getParameter("milliseconds");
            int seconds = secondsString != null ? Integer.valueOf(secondsString) : 0;
            String indefiniteString = req.getParameter("indefiniteBreak");
            boolean indefinite = indefiniteString != null ? Boolean.valueOf(indefiniteString) : false;
            String silentString = req.getParameter("silent");
            boolean silent = silentString != null ? Boolean.valueOf(silentString) : false;

            if (eventTypeString.equals("SetTime")) {
                timerEvent = new TimerEvent.SetTime(seconds);
            } else if (eventTypeString.equals("StopTime")) {
                timerEvent = new TimerEvent.StopTime(seconds);
            } else if (eventTypeString.equals("StartTime")) {
                timerEvent = new TimerEvent.StartTime(seconds, silent);
            } else if (eventTypeString.equals("BreakPaused")) {
                breakTimerEvent = new BreakTimerEvent.BreakPaused(seconds);
            } else if (eventTypeString.equals("BreakStarted")) {
                breakTimerEvent = new BreakTimerEvent.BreakStart(seconds, indefinite);
            } else if (eventTypeString.equals("BreakDone")) {
                breakTimerEvent = new BreakTimerEvent.BreakDone(null);
            } else if (eventTypeString.equals("BreakSetTime")) {
                breakTimerEvent = new BreakTimerEvent.BreakSetTime(seconds, indefinite);
            } else {
                String message = MessageFormat.format("unknown event type {0}", eventTypeString);
                this.logger.error(message);
                resp.sendError(400, message);
            }

            if (timerEvent != null) {
                timerEvent.setFopName(fopName);
                eventBus.post(timerEvent);
            }
            if (breakTimerEvent != null) {
                breakTimerEvent.setFopName(fopName);
                String mode = req.getParameter("mode");
                breakTimerEvent.setMode(mode);
                eventBus.post(breakTimerEvent);
            }

            if (defaultFopName == null) {
                defaultFopName = fopName;
            }
        } catch (NumberFormatException | IOException e) {
            this.logger.error(LoggerUtils.stackTrace(e));
        }
    }

}