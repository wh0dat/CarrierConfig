package com.android.carrierconfig;

import android.os.Build;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.service.carrier.CarrierIdentifier;
import android.service.carrier.CarrierService;
import android.text.TextUtils;
import android.util.Log;
import java.io.IOException;
import java.util.regex.Pattern;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class DefaultCarrierConfigService extends CarrierService {
    private XmlPullParserFactory mFactory = null;

    public DefaultCarrierConfigService() {
        Log.d("DefaultCarrierConfigService", "Service created");
    }

    public PersistableBundle onLoadConfig(CarrierIdentifier id) {
        Log.d("DefaultCarrierConfigService", "Config being fetched");
        if (id == null) {
            return null;
        }
        PersistableBundle config = new PersistableBundle();
        try {
            config.putAll(readConfigFromXml(getApplicationContext().getResources().getXml(R.xml.base), id));
        } catch (IOException | XmlPullParserException e) {
            Log.e("DefaultCarrierConfigService", e.toString());
        }
        try {
            synchronized (this) {
                if (this.mFactory == null) {
                    this.mFactory = XmlPullParserFactory.newInstance();
                }
            }
            XmlPullParser parser = this.mFactory.newPullParser();
            StringBuilder sb = new StringBuilder();
            sb.append("carrier_config_");
            sb.append(id.getMcc());
            sb.append(id.getMnc());
            sb.append(".xml");
            parser.setInput(getApplicationContext().getAssets().open(sb.toString()), "utf-8");
            config.putAll(readConfigFromXml(parser, id));
        } catch (IOException | XmlPullParserException e2) {
            Log.d("DefaultCarrierConfigService", e2.toString());
        }
        try {
            config.putAll(readConfigFromXml(getApplicationContext().getResources().getXml(R.xml.vendor), id));
        } catch (IOException | XmlPullParserException e3) {
            Log.e("DefaultCarrierConfigService", e3.toString());
        }
        return config;
    }

    static PersistableBundle readConfigFromXml(XmlPullParser parser, CarrierIdentifier id) throws IOException, XmlPullParserException {
        PersistableBundle config = new PersistableBundle();
        if (parser == null) {
            return config;
        }
        while (true) {
            int next = parser.next();
            int event = next;
            if (next == 1) {
                return config;
            }
            if (event == 2 && "carrier_config".equals(parser.getName()) && checkFilters(parser, id)) {
                config.putAll(PersistableBundle.restoreFromXml(parser));
            }
        }
    }

    static boolean checkFilters(XmlPullParser parser, CarrierIdentifier id) {
        boolean result = true;
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attribute = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);
            char c = 65535;
            boolean z = true;
            switch (attribute.hashCode()) {
                case -1581183623:
                    if (attribute.equals("customerid")) {
                        c = 7;
                        break;
                    }
                    break;
                case -1335157162:
                    if (attribute.equals("device")) {
                        c = 6;
                        break;
                    }
                    break;
                case -724768891:
                    if (attribute.equals("propcheck")) {
                        c = 9;
                        break;
                    }
                    break;
                case 107917:
                    if (attribute.equals("mcc")) {
                        c = 0;
                        break;
                    }
                    break;
                case 108258:
                    if (attribute.equals("mnc")) {
                        c = 1;
                        break;
                    }
                    break;
                case 114097:
                    if (attribute.equals("spn")) {
                        c = 4;
                        break;
                    }
                    break;
                case 3172527:
                    if (attribute.equals("gid1")) {
                        c = 2;
                        break;
                    }
                    break;
                case 3172528:
                    if (attribute.equals("gid2")) {
                        c = 3;
                        break;
                    }
                    break;
                case 3236474:
                    if (attribute.equals("imsi")) {
                        c = 5;
                        break;
                    }
                    break;
                case 164564499:
                    if (attribute.equals("carrierid")) {
                        c = 8;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    if (!result || !value.equals(id.getMcc())) {
                        z = false;
                    }
                    result = z;
                    break;
                case 1:
                    if (!result || !value.equals(id.getMnc())) {
                        z = false;
                    }
                    result = z;
                    break;
                case 2:
                    if (!result || !value.equalsIgnoreCase(id.getGid1())) {
                        z = false;
                    }
                    result = z;
                    break;
                case 3:
                    if (!result || !value.equalsIgnoreCase(id.getGid2())) {
                        z = false;
                    }
                    result = z;
                    break;
                case 4:
                    if (!result || !matchMultiAttributeValues(value, id.getSpn(), true)) {
                        z = false;
                    }
                    result = z;
                    break;
                case 5:
                    if (!result || !matchOnImsi(value, id)) {
                        z = false;
                    }
                    result = z;
                    break;
                case 6:
                    if (!result || !matchMultiAttributeValues(value, Build.DEVICE, false)) {
                        z = false;
                    }
                    result = z;
                    break;
                case 7:
                    if (!result || !matchMultiAttributeValues(value, SystemProperties.get("ro.mot.build.customerid", ""), true)) {
                        z = false;
                    }
                    result = z;
                    break;
                case 8:
                    if (!result || !value.equalsIgnoreCase(SystemProperties.get("ro.carrier", ""))) {
                        z = false;
                    }
                    result = z;
                    break;
                case 9:
                    if (!result || !propertyCheck(value)) {
                        z = false;
                    }
                    result = z;
                    break;
                default:
                    StringBuilder sb = new StringBuilder();
                    sb.append("Unknown attribute ");
                    sb.append(attribute);
                    sb.append("=");
                    sb.append(value);
                    Log.e("DefaultCarrierConfigService", sb.toString());
                    result = false;
                    break;
            }
        }
        return result;
    }

    static boolean matchOnImsi(String xmlImsi, CarrierIdentifier id) {
        String currentImsi = id.getImsi();
        if (currentImsi != null) {
            return Pattern.compile(xmlImsi, 2).matcher(currentImsi).matches();
        }
        return false;
    }

    static boolean matchMultiAttributeValues(String values, String MatchData, boolean useStrictComparation) {
        if (!TextUtils.isEmpty(values) && !TextUtils.isEmpty(MatchData)) {
            String[] valueArray = values.split("[|]");
            for (int i = 0; i < valueArray.length; i++) {
                if (useStrictComparation) {
                    if (MatchData.equalsIgnoreCase(valueArray[i])) {
                        return true;
                    }
                } else if (MatchData.toLowerCase().startsWith(valueArray[i].toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean propertyCheck(String criteria) {
        if (TextUtils.isEmpty(criteria)) {
            return false;
        }
        String[] pair = criteria.split("=");
        if (pair.length < 2) {
            return false;
        }
        String propName = pair[0];
        String propValues = pair[1];
        String runtimePropVal = SystemProperties.get(propName);
        String[] checkValueArray = propValues.split("[|]");
        boolean checkPass = false;
        for (String rule : checkValueArray) {
            boolean z = "EMPTY".equals(rule) ? checkPass || TextUtils.isEmpty(runtimePropVal) : "NOT_EMPTY".equals(rule) ? checkPass || !TextUtils.isEmpty(runtimePropVal) : checkPass || TextUtils.equals(rule, runtimePropVal);
            checkPass = z;
        }
        return checkPass;
    }
}
