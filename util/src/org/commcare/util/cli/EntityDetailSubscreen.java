package org.commcare.util.cli;

import org.commcare.suite.model.Detail;
import org.commcare.suite.model.DetailField;
import org.javarosa.core.model.condition.EvaluationContext;

import java.io.PrintStream;

/**
 * An entity detail subscreen displays one of the detail screens associated with an
 * entity list (or the only one if there is only one).
 *
 * It also provides navigation cues for switching which detail screen (or 'tab') is being
 * viewed.
 *
 * Created by ctsims on 8/20/2015.
 */
public class EntityDetailSubscreen extends Subscreen<EntityScreen> {
    EvaluationContext mSessionContext;

    private final int SCREEN_WIDTH = 100;

    private String[] rows;
    private DetailField[] mFields;
    private String[] mDetailListTitles;

    private int mCurrentIndex;

    public EntityDetailSubscreen(int currentIndex, Detail detail, EvaluationContext subContext, String[] detailListTitles) {
        mFields = detail.getFields();
        rows = new String[mFields.length];

        for(int i = 0 ; i < mFields.length ; ++i) {
            rows[i] = createRow(mFields[i], subContext);
        }
        mDetailListTitles = detailListTitles;

        mCurrentIndex = currentIndex;
    }

    private String createRow(DetailField field, EvaluationContext ec) {
        StringBuilder row = new StringBuilder();
        String header = field.getHeader().evaluate(ec);

        addPaddedStringToBuilder(row, header, SCREEN_WIDTH / 2 );
        row.append(" | ");

        String value;
        Object o = field.getTemplate().evaluate(ec);
        if(!(o instanceof String)) {
            value = "{ " + field.getTemplateForm() + " data}";
        } else {
            value = (String)o;
        }
        addPaddedStringToBuilder(row, value, SCREEN_WIDTH / 2);

        return row.toString();
    }
    private void addPaddedStringToBuilder(StringBuilder builder, String s, int width) {
        if (s.length() > width) {
            builder.append(s, 0, width);
            return;
        }
        builder.append(s);
        if (s.length() != width) {
            // add whitespace padding
            for (int i = 0; i < width - s.length(); ++i) {
                builder.append(' ');
            }
        }
    }

    private String pad(String s, int width) {
        StringBuilder builder = new StringBuilder();
        addPaddedStringToBuilder(builder, s, width);
        return builder.toString();
    }

    @Override
    public void prompt(PrintStream out) {
        boolean multipleInputs = false;
        if(mDetailListTitles.length > 1) {
            createTabHeader(out);
            out.println("==============================================================================================");
            multipleInputs = true;
        }

        for (int i = 0; i < rows.length; ++i) {
            String row = rows[i];
            out.println(row);
        }

        String msg;
        if(multipleInputs) {
            msg = "Press enter to select this case, or the number of the detail tab to view";
        } else {
            msg = "Press enter to select this case";
        }
        out.println();
        out.println(msg);
    }

    private void createTabHeader(PrintStream out) {
        StringBuilder sb = new StringBuilder();
        int widthPerTab = (int)(SCREEN_WIDTH * 1.0 / mDetailListTitles.length);
        for(int i = 0 ; i < mDetailListTitles.length; ++i) {
            String title = i + ") " + mDetailListTitles[i];
            if(i == this.mCurrentIndex) {
                title = "[" + title + "]";
            }
            this.addPaddedStringToBuilder(sb, title, widthPerTab);
        }
        out.println(sb.toString());
    }


    @Override
    public boolean handleInputAndUpdateHost(String input, EntityScreen host) {
        if(input.trim().equals("")) {
            return true;
        }
        try {
            int i = Integer.parseInt(input);
            if(i >= 0 && i < mDetailListTitles.length) {
                host.setCurrentScreenToDetail(i);
                return false;
            }
        } catch (NumberFormatException e) {
            //This will result in things just executing again, which is fine.
        }
        return false;
    }
}
