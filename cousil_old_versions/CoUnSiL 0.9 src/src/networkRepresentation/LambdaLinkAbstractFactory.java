package networkRepresentation;

/**
 * Created with IntelliJ IDEA.
 * User: Martin Pulec
 * Date: 5.9.13
 * Time: 17:51
 * To change this template use File | Settings | File Templates.
 */
public interface LambdaLinkAbstractFactory {
        public void allocate(LambdaLink lambdaLink);
        public void deallocate(LambdaLink lambdaLink);
        public void modify(LambdaLink lambdaLink);
        public void query(LambdaLink lambdaLink);
        public void queryAndModify(LambdaLink lambdaLink);
}
