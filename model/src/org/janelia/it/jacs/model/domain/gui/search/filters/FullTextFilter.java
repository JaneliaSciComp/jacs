package org.janelia.it.jacs.model.domain.gui.search.filters;

public class FullTextFilter implements Filter {

    private String text;

    @Override
    public String getLabel() {
        return "Search: "+text;
    }

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
