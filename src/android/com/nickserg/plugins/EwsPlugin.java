package com.epam.plugins;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.TimeZone;


import microsoft.exchange.webservices.data.Appointment;
import microsoft.exchange.webservices.data.AppointmentSchema;
import microsoft.exchange.webservices.data.CancelMeetingMessage;
import microsoft.exchange.webservices.data.DeleteMode;
import microsoft.exchange.webservices.data.SendCancellationsMode;
import microsoft.exchange.webservices.data.CalendarFolder;
import microsoft.exchange.webservices.data.CalendarView;
import microsoft.exchange.webservices.data.EmailAddress;
import microsoft.exchange.webservices.data.ExchangeCredentials;
import microsoft.exchange.webservices.data.ExchangeService;
import microsoft.exchange.webservices.data.FindItemsResults;
import microsoft.exchange.webservices.data.FolderId;
import microsoft.exchange.webservices.data.Item;
import microsoft.exchange.webservices.data.ItemSchema;
import microsoft.exchange.webservices.data.Mailbox;
import microsoft.exchange.webservices.data.PropertySet;
import microsoft.exchange.webservices.data.SendInvitationsMode;
import microsoft.exchange.webservices.data.WebCredentials;
import microsoft.exchange.webservices.data.WellKnownFolderName;

public class EwsPlugin extends CordovaPlugin
{
    public static final String ACTION_GET_ROOMS = "getRooms";	
    public static final String ACTION_GET_APPOINTMENTS = "getAppointments";
    public static final String ACTION_CREATE_APPOINTMENT = "createAppointment";
    public static final String ACTION_CANCEL_APPOINTMENT = "cancelAppointment";

    private static DateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static ExchangeService _service;
    private URI url;
    private CallbackContext context;
    private String email;
    private String password;

    public EwsPlugin() throws URISyntaxException {
        url = new URI("https://owamsq.epam.com/ews/exchange.asmx");
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException
    {        
        context = callbackContext;	
        JSONObject config = args.getJSONObject(0);
        email = config.getString("email");
        password = config.getString("password");
        if (ACTION_GET_ROOMS.equals(action))
            return getRooms(config.getString("roomRootEmail"));
        else if(ACTION_GET_APPOINTMENTS.equals(action))
            return getAppointments(config.getString("roomEmail"), config.getString("start"), config.getString("end"));
        else if(ACTION_CANCEL_APPOINTMENT.equals(action))
            return cancelAppointment(config.getString("roomEmail"), config.getString("start"), config.getString("end"), config.getString("uniqueId"), config.getString("changeKey"));
        else if(ACTION_CREATE_APPOINTMENT.equals(action))
        {
            return createAppointment(
                    config.getString("subject"),
                    config.getString("start"),
                    config.getInt("duration"),
                    config.getString("location"),
                    config.getString("address"));
        }

        return false;
    }

    protected boolean getRooms(String roomRootEmail)
    {        
        try {
            ExchangeService service = getService();
            Collection<EmailAddress> rooms = service.getRooms(new EmailAddress(roomRootEmail));   
            JSONArray result = new JSONArray();            
            for(EmailAddress emailAddress : rooms){
                JSONObject roomJSON = new JSONObject();
                roomJSON.put("displayName", emailAddress.getName());
                roomJSON.put("email", emailAddress.getAddress());
                result.put(roomJSON.toString());
            }
            context.success(result.toString());
        } catch (Exception e) {
            context.error(e.getMessage());
        }                      
        
        return true;
    }
    
    protected boolean getAppointments(String roomEmail, String start, String end){                
        try {
            ExchangeService service = getService();
            FolderId folderId = new FolderId(WellKnownFolderName.Calendar, new Mailbox(roomEmail));
            CalendarFolder calendarFolder = CalendarFolder.bind(service, folderId);
            dataFormat.setTimeZone(TimeZone.getDefault());
            Date startDate = dataFormat.parse(start);
            Date endDate = dataFormat.parse(end);
            CalendarView calView = new CalendarView(startDate, endDate);
            calView.setPropertySet(new PropertySet(ItemSchema.Subject, AppointmentSchema.Organizer, AppointmentSchema.Start, AppointmentSchema.End));
            FindItemsResults<Appointment> appointments = calendarFolder.findAppointments(calView);   
            JSONArray result = new JSONArray();
            for(Item item : appointments){

                Appointment appointment = (Appointment) item;

                EmailAddress org = appointment.getOrganizer();
                JSONObject appointmentJSON = new JSONObject();
                appointmentJSON.put("location", roomEmail);
                appointmentJSON.put("subject", appointment.getSubject());
                appointmentJSON.put("organizer", org.getName());
                appointmentJSON.put("orgEmail", org.getAddress());
                appointmentJSON.put("start", dataFormat.format(appointment.getStart()));
                appointmentJSON.put("end", dataFormat.format(appointment.getEnd()));
                appointmentJSON.put("uniqueId", appointment.getId().getUniqueId());
                appointmentJSON.put("changeKey", appointment.getId().getChangeKey());
                result.put(appointmentJSON.toString());
            }
            context.success(result.toString());
        } catch (Exception e) {
            context.error(e.getMessage());
        }                      
        return true;
    }



    protected boolean cancelAppointment(String roomEmail, String start, String end, String uniqueId, String changeKey){
            try {
                ExchangeService service = getService();

                FolderId folderId = new FolderId(WellKnownFolderName.Calendar, new Mailbox(email));

                CalendarFolder calendarFolder = CalendarFolder.bind(service, folderId);
                dataFormat.setTimeZone(TimeZone.getDefault());

                Date startDate = dataFormat.parse(start);
                Date endDate = dataFormat.parse(end);

                CalendarView calView = new CalendarView(startDate, endDate);

                calView.setPropertySet(new PropertySet(ItemSchema.Subject, AppointmentSchema.Organizer, AppointmentSchema.Start, AppointmentSchema.End));

                FindItemsResults<Appointment> appointments = calendarFolder.findAppointments(calView);

                JSONArray result = new JSONArray();

                if (appointments.getTotalCount()<=0){
                    context.error("You cannot cancel meeting");
                    return true;
                }

                for(Item item : appointments){

                    Appointment appointment = (Appointment) item;

                    CancelMeetingMessage cancelMessage = appointment.createCancelMeetingMessage();
                    cancelMessage.sendAndSaveCopy();

                }
                context.success(result.toString());
            } catch (Exception e) {
                context.error(e.getMessage());
            }
            return true;
        }

    protected boolean createAppointment(String subject, String start, int duration, String location, String address){
        try {    
            ExchangeService service = getService(true);      
            dataFormat.setTimeZone(TimeZone.getDefault());
            Date startDate = dataFormat.parse(start);
            Appointment appointment = new Appointment(service);
            appointment.setSubject(subject);
            appointment.setStart(startDate);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startDate);
            calendar.add(Calendar.MINUTE, duration);
            calendar.add(Calendar.SECOND, -30);

            appointment.setEnd(calendar.getTime());
            appointment.setLocation(location);
            appointment.getResources().add(address);

            appointment.save(SendInvitationsMode.SendOnlyToAll);
            context.success();
        } catch (Exception e) {
            context.error(e.getMessage());
        }           
        
        return true;
    }
    
    private ExchangeService getService(Boolean reconnect) throws Exception {
        if(!reconnect && _service != null)
            return _service;
        
        try {
            _service = new ExchangeService();
            ExchangeCredentials credentials = new WebCredentials(email, password);
            _service.setCredentials(credentials);
            if(url != null)
                _service.setUrl(url);
            else
            {
                _service.autodiscoverUrl(email);
                url = _service.getUrl();
            }
        }
        catch (Exception e){
            url = null;
            _service = null;
            throw e;
        }
        
        return _service;
    }
    
    private ExchangeService getService() throws Exception{
        return getService(false);
    }
}