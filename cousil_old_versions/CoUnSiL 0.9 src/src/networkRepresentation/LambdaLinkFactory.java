package networkRepresentation;

/**
 * Created by IntelliJ IDEA.
 * User: xliska
 * Date: 15.1.2009
 * Time: 15:15:59
 */
public class LambdaLinkFactory {
    private LambdaLinkAbstractFactory factory;

    public LambdaLinkFactory(LambdaLink lambdaLink) {
        switch (lambdaLink.getType()) {
            case NSI1:
                factory = new LambdaLinkFactoryImplNSI1(lambdaLink);
                break;
            case NSI2:
                factory = new LambdaLinkFactoryImplNSI2(lambdaLink);
                break;
            case OSCARS:
                factory = new LambdaLinkFactoryImplOSCARS(lambdaLink);
                break;
        }
    }

    public void allocate(LambdaLink lambdaLink) {
        factory.allocate(lambdaLink);
    }

    public void deallocate(LambdaLink lambdaLink) {
        factory.deallocate(lambdaLink);
    }

    public void modify(LambdaLink lambdaLink) {
        factory.modify(lambdaLink);
    }

    public void query(LambdaLink lambdaLink) {
        factory.query(lambdaLink);
    }

    public void queryAndModify(LambdaLink lambdaLink) {
        factory.queryAndModify(lambdaLink);
    }
}
