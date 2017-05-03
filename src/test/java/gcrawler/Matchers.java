package gcrawler;

import org.mockito.ArgumentMatcher;

import java.util.List;

/**
 * Support classes.
 */
class Matchers {

    /**
     * A Mockito matcher for one-element lists.
     */
    static class IsSingleElementList extends ArgumentMatcher<List<String>> {
        @Override
        public boolean matches(Object arg) {
            return ((List<String>) arg).size() == 1;
        }
    }

    /**
     * A Mockito matcher for empty lists.
     */
    static class IsEmptyList extends ArgumentMatcher<List<String>> {
        @Override
        public boolean matches(Object arg) {
            return ((List<String>) arg).isEmpty();
        }
    }

}
